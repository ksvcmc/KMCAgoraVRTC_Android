# 金山云魔方连麦API文档

## 项目背景
金山魔方是一个多媒体能力提供平台，通过统一接入API、统一鉴权、统一计费等多种手段，降低客户接入多媒体处理能力的代价，提供多媒体能力供应商的效率。  
本文档主要针对视频连麦功能而说明
## 效果展示
![Alt text](https://raw.githubusercontent.com/wiki/ksvcmc/KMCAgoraVRTC_Android/lianmai.jpg)
   
## 目录结构
**demo**: 示例工程  
**libs**: 魔方sdk包libkmcagoravrtc.jar，以及声网sdk包  

**注: demo工程使用软链接引用libs目录，对于windows平台做Android开发的用户，需要手动将libs目录拷贝到demo目录下。**

此外，由于本sdk只封装了连麦相关的功能，如需要推流的能力，需要集成其他推流SDK。  
demo中演示了与金山云推流SDK集成的方法，因此工程中添加了对libksylive库.

## 导入SDK
引入目标库, 将libs目录下的库文件引入到目标工程中并添加依赖。

可参考下述配置方式（以Android Studio为例）：
 +  推荐直接使用gradle方式集成：
  
    ```
       allprojects {
         repositories {
             jcenter()
       }
       
       dependencies {
        compile 'com.ksyun.mc:KMCAgoraVRTC:1.0.1'
       }
    ```

 +  手动下载集成
   将libs目录copy到目标工程的根目录下；
   修改目标工程的build.gradle文件，配置jniLibs路径：
   
    ```
    sourceSets {
        main {
            jniLibs.srcDirs = ['libs']
        }
    }
    ```

## 鉴权
SDK在使用时需要用token进行鉴权后方可使用，token申请方式见**接入步骤**部分;  
token与应用包名为一一对应的关系;

## SDK使用指南

本sdk使用简单，初次使用需要在魔方服务后台申请token，用于客户鉴权，使用下面的接口鉴权
```java
void authorize(String token, KMCAuthResultListener listener)
```

加入一个频道

```java
/**
  * @channel 标识通话的频道名称，长度在64字节以内的字符串
  * @uid 用户ID，32位无符号整数。建议设置范围：1到(2^32-1)，并保证唯一性。
  *      如果不指定（即设为0），SDK 会自动分配一个
  */
void joinChannel(String channel, int uid)
```

离开频道，即挂断或退出通话

```java
void leaveChannel()
```

本地视频数据使用下面的接口发送到远端

```java
/**
  * @buf 视频数据
  * @width 宽
  * @height 高
  * @orientation 旋转角度
  * @pts 时间戳
  */
void sendVideoFrame(byte[] buf, int width, int height,
                               int orientation, long pts)
```

远端视频数据回调

```java
/**
  * @buffer 视频数据
  * @format 视频参数
  * @pts 时间戳
  */
void onReceiveRemoteVideoFrame(ByteBuffer buffer, VideoFormat format, long pts)
```

本地音频数据回调

```java
/**
  * @buffer 音频数据
  * @format 音频参数
  * @pts 时间戳
  */
void onReceiveLocalAudioFrame(ByteBuffer buffer, AudioFormat format, long pts)
```


远端音频数据回调

```java
/**
  * @buffer 音频数据
  * @format 音频参数
  * @pts 时间戳
  */
void onReceiveRemoteAudioFrame(ByteBuffer buffer, AudioFormat format, long pts) 
```

设置视频profile
```java
/**
  * @profile 视频pfile
  * @swap 是否交换宽高
  */
void setVideoProfile(int profile, boolean swap) 
```

注册事件回调

```java
/**
  * @listener 事件回调
  */
void registerEventListener(KMCAgoraEventListener listener)
```

本sdk只封装了连麦相关的功能，可结合金山云推流sdk使用，完成音视频的合成、推流等操作。  
demo工程给出了魔方视频连麦 + 金山云推流的示例， 封装了KMCAgoraStreamer类和KMCAgoraVRTCClient类，具体可参考demo代码。

## 接入流程
![金山魔方接入流程](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_Android/all.jpg "金山魔方接入流程")
## 接入步骤  
1.登录[金山云控制台]( https://console.ksyun.com)，选择视频服务-金山魔方
![步骤1](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_Android/step1.png "接入步骤1")

2.在金山魔方控制台中挑选所需服务。
![步骤2](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_Android/step2.png "接入步骤2")

3.点击申请试用，填写申请资料。
![步骤3](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_Android/step3.png "接入步骤3")

![步骤4](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_Android/step4.png "接入步骤4")

4.待申请审核通过后，金山云注册时的邮箱会收到邮件及试用token。
![步骤5](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_Android/step5.png "接入步骤5")

5.下载安卓/iOS版本的SDK集成进项目。
![步骤6](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_Android/step6.png "接入步骤6")

6.参照文档和DEMO填写TOKEN，就可以Run通项目了。  
7.试用中或试用结束后，有意愿购买该服务可以与我们的商务人员联系购买。  
（商务Email:KSC-VBU-KMC@kingsoft.com）
## Demo下载
![Alt text](https://raw.githubusercontent.com/wiki/ksvcmc/KMCAgoraVRTC_Android/lianmaicode.png)
## 反馈与建议
主页：https://docs.ksyun.com/read/latest/142/_book/index.html  
邮箱：ksc-vbu-kmc-dev@kingsoft.com  
QQ讨论群：574179720 [视频云技术交流群]  
