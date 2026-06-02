#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

uniform vec2 OutSize;

// 桶形畸变系数（模拟鱼眼镜头）
const float k = -0.22;
const float kcube = -0.08;

void main() {
    // 将纹理坐标转换为以中心为原点的坐标 [-0.5, 0.5]
    vec2 uv = texCoord - 0.5;
    
    // 修正宽高比以保证桶形畸变的完美圆形对称
    float aspect = OutSize.x / OutSize.y;
    uv.x *= aspect;
    
    // 计算中心距离平方
    float r2 = uv.x * uv.x + uv.y * uv.y;
    
    // 桶形畸变映射公式
    float f = 1.0 + k * r2 + kcube * r2 * r2;
    
    // 映射回纹理坐标区间 [0.0, 1.0]
    vec2 distorted_uv = uv * f;
    distorted_uv.x /= aspect;
    distorted_uv += 0.5;
    
    // 1. 模拟圆形镜头外壳遮罩（超出画面物理畸变范围的渲染为黑色）
    if (distorted_uv.x < 0.0 || distorted_uv.x > 1.0 || distorted_uv.y < 0.0 || distorted_uv.y > 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    } else {
        // 采集 Minecraft 原场景像素颜色
        vec4 color = texture(DiffuseSampler, distorted_uv);
        
        // 2. 光学暗角（越靠近边缘越暗，模拟真实镜头发散）
        float vignette = 1.0 - r2 * 0.35;
        vignette = clamp(vignette, 0.0, 1.0);
        
        // 3. 微量模拟图传扫描线（CRT/扫描感，为图传风增色）
        float scanline = sin(distorted_uv.y * OutSize.y * 3.14159) * 0.04;
        vec3 finalColor = color.rgb * vignette * (1.0 - scanline);
        
        fragColor = vec4(finalColor, 1.0);
    }
}
