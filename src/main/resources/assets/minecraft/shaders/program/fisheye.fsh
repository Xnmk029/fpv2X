#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

uniform vec2 OutSize;

// 广角镜头畸变参数 (等距球面投影模型)
const float A = 1.15;        // 畸变强度 (等效半视场角，值越大鱼眼感越强)
const float tan_A = 1.5574;  // tan(A) 的预计算值，优化性能
const float S = 0.85;        // 整体画面缩放/拉远系数 (值越小 FOV 越大，中心越小)

void main() {
    // 将纹理坐标转换为以中心为原点的坐标 [-0.5, 0.5]
    vec2 uv = texCoord - 0.5;
    
    // 修正宽高比以保证桶形畸变的完美圆形对称
    float aspect = OutSize.x / OutSize.y;
    vec2 uv_scaled = vec2(uv.x * aspect, uv.y);
    
    // 计算中心距离 (物理距离)
    float rf = length(uv_scaled);
    
    // 计算非线性等距鱼眼映射比例
    // 当 rf -> 0 时，根据极限，tan(rf*A)/(rf*tan(A)) 逼近 A/tan(A)
    float scale = (rf > 0.0) ? (tan(rf * A) / (rf * tan_A)) : (A / tan_A);
    scale *= S;
    
    // 映射回原始采样纹理坐标区间 [0.0, 1.0]
    vec2 distorted_uv = uv * scale + 0.5;
    
    // 1. 模拟圆形镜头外壳遮罩（超出画面物理畸变范围的渲染为黑色）
    if (distorted_uv.x < 0.0 || distorted_uv.x > 1.0 || distorted_uv.y < 0.0 || distorted_uv.y > 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    } else {
        // 采集 Minecraft 原场景像素颜色
        vec4 color = texture(DiffuseSampler, distorted_uv);
        
        // 2. 光学暗角（越靠近边缘越暗，模拟真实镜头发散）
        float vignette = 1.0 - rf * rf * 0.35;
        vignette = clamp(vignette, 0.0, 1.0);
        
        // 3. 微量模拟图传扫描线（CRT/扫描感，为图传风增色）
        float scanline = sin(distorted_uv.y * OutSize.y * 3.14159) * 0.04;
        vec3 finalColor = color.rgb * vignette * (1.0 - scanline);
        
        fragColor = vec4(finalColor, 1.0);
    }
}
