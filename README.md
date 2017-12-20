# 金山云魔方连麦API文档

## 项目背景
金山魔方是一个多媒体能力提供平台，通过统一接入API、统一鉴权、统一计费等多种手段，降低客户接入多媒体处理能力的代价，提供多媒体能力供应商的效率。  
本文档主要针对视频连麦功能而说明
## 效果展示
### 默认连麦模式(大小窗口)效果
![Alt text](https://raw.githubusercontent.com/wiki/ksvcmc/KMCAgoraVRTC_Android/lianmai.jpg)

### PK连麦效果
![pk_demo](https://raw.githubusercontent.com/wiki/ksvcmc/KMCAgoraVRTC_Android/image/pk_demo.jpg)

## 目录结构
**demo**: 示例工程  
**libs**: 魔方sdk包libkmcagoravrtc.jar，以及声网sdk包  

**注: demo工程使用软链接引用libs目录，对于windows平台做Android开发的用户，需要手动将libs目录拷贝到demo目录下。**

此外，由于本sdk只封装了连麦相关的功能，如需要推流的能力，需要集成其他推流SDK。  
demo中演示了与金山云推流SDK集成的方法，因此工程中添加了对libksylive库.

## 鉴权
SDK在使用时需要用token进行鉴权后方可使用，token申请方式见**接入步骤**部分;  
token与应用包名为一一对应的关系;  
鉴权错误码见：https://github.com/ksvcmc/KMCAgoraVRTC_Android/wiki/auth_error

## SDK使用指南
SDK使用指南请见[wiki](https://github.com/ksvcmc/KMCAgoraVRTC_Android/wiki)

## 接入流程
详情请见[接入说明](https://github.com/ksvcmc/KMCAgoraVRTC_Android/wiki/token_apply)

## Demo下载
![Alt text](https://raw.githubusercontent.com/wiki/ksvcmc/KMCAgoraVRTC_Android/lianmaicode.png)

## 反馈与建议
主页：https://docs.ksyun.com/read/latest/142/_book/index.html  
邮箱：ksc-vbu-kmc-dev@kingsoft.com  
QQ讨论群：574179720 [视频云技术交流群]  
