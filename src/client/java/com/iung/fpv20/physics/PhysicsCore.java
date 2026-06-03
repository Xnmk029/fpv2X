package com.iung.fpv20.physics;

import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import static com.iung.fpv20.utils.LocalMath.DEG_TO_RAD;
import static com.iung.fpv20.utils.LocalMath.RAD_TO_DEG;

public class PhysicsCore {

    /**
     * 通过指数映射（Exponential Map）构造单步增量四元数，实现三轴同时旋转更新。
     * 
     * 数学原理：
     *   旋转向量 theta = omega_body * dt（弧度）
     *   增量四元数 delta_q = [cos(|theta|/2), sin(|theta|/2) * theta / |theta|]
     *   核心四元数更新 q_new = q_old * delta_q（右乘 = 机体局部旋转）
     *
     * @param q            当前核心四元数（就地修改并返回）
     * @param roll_deg_s   横滚角速度（度/秒），对应机体 Z 轴
     * @param pitch_deg_s  俯仰角速度（度/秒），对应机体 X 轴
     * @param yaw_deg_s    偏航角速度（度/秒），对应机体 Y 轴
     * @param dt           时间步长（秒）
     * @return 更新并归一化后的核心四元数
     */
    public static Quaternionf rotate_by_angular_velocity(
            Quaternionf q,
            float roll_deg_s, float pitch_deg_s, float yaw_deg_s,
            float dt
    ) {
        // 1. 构造旋转向量（弧度增量）：JOML 坐标系 X=Pitch, Y=Yaw, Z=Roll
        float rx = pitch_deg_s * dt * DEG_TO_RAD;
        float ry = yaw_deg_s   * dt * DEG_TO_RAD;
        float rz = roll_deg_s  * dt * DEG_TO_RAD;

        // 2. 计算旋转向量模长的一半
        float angle = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        float halfAngle = angle * 0.5f;

        // 3. 通过指数映射构造增量四元数
        float dqx, dqy, dqz, dqw;
        if (halfAngle > 1e-6f) {
            float sinHalf = (float) Math.sin(halfAngle);
            float scale = sinHalf / angle; // = sin(|theta|/2) / |theta|
            dqx = rx * scale;
            dqy = ry * scale;
            dqz = rz * scale;
            dqw = (float) Math.cos(halfAngle);
        } else {
            // 小角度线性近似: sin(x)/x ≈ 1, cos(x) ≈ 1
            dqx = rx * 0.5f;
            dqy = ry * 0.5f;
            dqz = rz * 0.5f;
            dqw = 1.0f;
        }

        // 4. 左乘增量四元数（由于 JOML 中 rotateLocalX 等方法实际上是左乘，即 dq * q）：q_new = delta_q * q
        //    使用 Hamilton 乘法展开以避免创建临时对象
        float qx = q.x, qy = q.y, qz = q.z, qw = q.w;
        q.set(
            dqw * qx + dqx * qw + dqy * qz - dqz * qy,
            dqw * qy - dqx * qz + dqy * qw + dqz * qx,
            dqw * qz + dqx * qy - dqy * qx + dqz * qw,
            dqw * qw - dqx * qx - dqy * qy - dqz * qz
        );

        // 5. 归一化，防止数值漂移
        q.normalize();

        return q;
    }

    /**
     * 向后兼容方法：内部委托给 rotate_by_angular_velocity。
     */
    public static Quaternionf rotate_from_local_yaw_pitch_roll(
            Quaternionf dist,
            float yaw, float pitch, float roll,
            float yaw_a_deg_s, float pitch_a_deg_s, float roll_a_deg_s,
            float dt
    ) {
        return rotate_by_angular_velocity(
                dist,
                roll * roll_a_deg_s,
                pitch * pitch_a_deg_s,
                yaw * yaw_a_deg_s,
                dt
        );
    }

    public static Quaternionf from_ypr_deg(float yaw_deg, float pitch_deg, float roll_deg) {
        Quaternionf q = new Quaternionf();
        q.rotateZ(roll_deg * DEG_TO_RAD);
        q.rotateX(pitch_deg * DEG_TO_RAD);
        q.rotateY((yaw_deg + 180.0F) * DEG_TO_RAD);
        return q;
    }

    public static Vector3f from_quaternion_to_ypr_deg(Quaternionf q) {
        Vector3f x_y_z = q.getEulerAnglesZXY(new Vector3f());
        return new Vector3f(x_y_z.y * RAD_TO_DEG + 180, x_y_z.x * RAD_TO_DEG, x_y_z.z * RAD_TO_DEG);
    }
}
