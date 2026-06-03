# FPV2X

[English](#english) | [简体中文](#简体中文)

<a id="english"></a>

## description

another fpv mod for Minecraft 1.20.1 fabric

**FPV2X** is an upgraded high-fidelity fork of the original [fpv20](https://github.com/wefcdse/fpv20) project.

what has been upgraded in FPV2X:

- **high-fidelity physical simulation**: implemented Uncrashed-style propeller aerodynamics, frame dimensions, motors, battery specs, and camera payload configurations.
- **realistic visual effects**: resolved and integrated GPU-accelerated Fisheye lens post-processing with analog static vignette.
- **flight controller model selection**: integrated dynamic Rates toggle directly in options supporting Betaflight, Actual, and KISS algorithms.

it can also run in forge
by [Sinytra Connector](https://www.curseforge.com/minecraft/mc-mods/sinytra-connector).
when used in forge, you need to set `is_in_forge` to `true` in `config/fpv20_common.json`

inspired by [Minecraft FPV](https://www.curseforge.com/minecraft/mc-mods/fpv-drone)

## links

[github (original)](https://github.com/wefcdse/fpv20)

[video](https://www.bilibili.com/video/BV1o4421c7Ek/)

## usage

client only:

- set `client_only` in `config/fpv20_common.json` to `true`

the gui entry is in options - controls

playing fpv:

- select a controller
- set the sticks' channel name to:
  - throttle: `t`
  - yaw: `y`
  - pitch: `p`
  - roll: `r`
- set a switch's channel name to `sw`
- set throttle's calibration type to `max min`
- enter a world, turn the switch. and enjoy flying!

slow motion

- set one channel's name to the same as `config/fpv20_client.json/slow_motion_switch_name`
- when this channel's value is over 0.5, it starts slow motion,
  the drone will fly slow.

use the receiver block:

- select a controller
- enter a world
- put down a `receiver block`
- click on the `receiver block`
- set the channel to receive in the GUI
- set whether to receive the negative or
  the positive half of the channel.
  the output is always `abs(signal) * 15`

toggling the sticks on the screen (OSD):

- use the keybind (by default `O`)
- use the button in the fpv setting

## why you should use FPV2X

- you want to play in 1.20.4 and fabric (or forge, using [Sinytra Connector](https://www.curseforge.com/minecraft/mc-mods/sinytra-connector))
- you want realistic physical simulation & hardware tuning (Uncrashed physical engine style)
- you want realistic visual effects such as barrel distortion and analog noise (Fisheye Lens)
- you want flight controller algorithm support (Betaflight, Actual, KISS)
- you want a clean, organized, and easy to use GUI
- you want to use a controller's input as a redstone signal

## advertisement

[simply snow](https://www.curseforge.com/minecraft/mc-mods/simply-snow)

---

<a id="简体中文"></a>

# FPV2X (简体中文)

## description

基于 Minecraft 1.20.1 fabric 的无人机 FPV 模拟模组。

**FPV2X** 是基于原版 [fpv20](https://github.com/wefcdse/fpv20) 项目深度开发与重构的高保真物理升级版。

FPV2X 中新增与改进的内容：

- **高保真空气动力学仿真**：实现了类似 Uncrashed 的桨叶气动模型，支持机架尺寸、电机选型、电池规格、挂载相机重量以及桨叶直径和螺距的深度调参。
- **超逼真视觉后处理特效**：修复并启用了 GPU 加速的鱼眼模拟图传镜头特效，带有精美的光学暗角与模拟高频扫描线。
- **飞控 Rates 模型动态切换**：在设置界面中支持一键切换 Betaflight、Actual 和 KISS 飞控速率解算算法，实时重算生效。

它也可以通过 [Sinytra Connector](https://www.curseforge.com/minecraft/mc-mods/sinytra-connector) 运行在 forge 中。
当在 forge 中使用时，你需要在 `config/fpv20_common.json` 中将 `is_in_forge` 设置为 `true`。

灵感来源于 [Minecraft FPV](https://www.curseforge.com/minecraft/mc-mods/fpv-drone)

## links

[github (原项目)](https://github.com/wefcdse/fpv20)

[视频介绍](https://www.bilibili.com/video/BV1o4421c7Ek/)

## usage

仅客户端模式：

- 在 `config/fpv20_common.json` 中将 `client_only` 设置为 `true`

GUI 入口在：选项 - 控制

开始飞行 FPV：

- 选择一个遥控器/控制器
- 将摇杆通道名称设置为：
  - 油门：`t`
  - 航向：`y`
  - 俯仰：`p`
  - 横滚：`r`
- 将一个开关的通道名称设置为 `sw`
- 将油门的校准类型设置为 `max min`
- 进入世界，拨动开关，尽情享受飞行！

慢动作

- 将一个通道名称设置为与 `config/fpv20_client.json/slow_motion_switch_name` 相同的值
- 当该通道的值超过 0.5 时，开始慢动作，无人机会飞得很慢。

使用信号接收器方块：

- 选择一个遥控器/控制器
- 进入世界
- 放下一个 `receiver block` (信号接收器方块)
- 点击该方块
- 在 GUI 中设置要接收的通道
- 设置接收通道的正半部分还是负半部分。
  输出始终为 `abs(signal) * 15`

在屏幕上切换摇杆显示 (OSD)：

- 使用快捷键（默认是 `O`）
- 使用 FPV 设置界面中的开关

## why you should use FPV2X

- 你想在 1.20.4 和 fabric（或 forge，通过 [Sinytra Connector](https://www.curseforge.com/minecraft/mc-mods/sinytra-connector)）中游玩
- 你想要高保真的空气动力学物理模拟与硬件规格调参（Uncrashed 调参风格）
- 你想要逼真的图传镜头效果（鱼眼畸变、光学暗角及模拟高频扫描线）
- 你想要支持专业的飞控算法（Betaflight、Actual、KISS）
- 你想要一个干净、整洁、易于使用的 GUI 界面
- 你想使用遥控器的输入来作为红石信号

## advertisement

[simply snow](https://www.curseforge.com/minecraft/mc-mods/simply-snow)
