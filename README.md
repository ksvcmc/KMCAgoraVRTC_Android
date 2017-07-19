# 金山云 - Agora声网连麦

金山云 - Agora声网连麦基于[金山云推流SDK](https://github.com/ksvc/KSYStreamer_Android)，集成了Agora声网连麦相关的功能。  
本Demo的集成方式采用了Agora的模式B，详细参考[Agora连麦](https://docs.agora.io/cn/user_guide/live_broadcast/host_in.html)

## 一. 功能特点

### 连麦功能
* 在 [金山云推流SDK](https://github.com/ksvc/KSYStreamer_Android)增加了连麦功能。

## 二. 运行环境

* 最低支持版本为Android 4.4

## 三. 开发指南

* 运行demo前请到[声网](https://dashboard.agora.io) 申请自己的app id,
  替换src/main/res/values/strings.xml中app_id的值

* 基于金山云推流SDK，demo定义了继承自KSYStreamer的kit类[KSYAgoraStreamer](https://github.com/ksvc/KSYDiversityLive_Android/blob/master/Agora/demo/src/main/java/com/ksyun/media/agora/kit/KSYAgoraStreamer.java)
实现直播连麦功能。
  - KSYAgoraStreamer直播推流、美颜、录制等功能同金山云推流SDK，详细使用指南请参考[KSYStramer说明](https://github.com/ksvc/KSYStreamer_Android/wiki)
  - KSYAgoraStreamer连麦功能接口

      开始连麦
      ```java
      void startRTC()
      ```

      结束连麦
      ```java
      void stopRTC()
      ```

      连麦小窗口位置和大小设置
      ```java
      void setRTCSubScreenRect(float left, float top, float width, float height, int mode)
      ```

      设置连麦时主屏幕类型
      ```java
      void setRTCMainScreen(int mainScreenType)
      ```
      
## 四. 快速集成  

<img src="https://raw.githubusercontent.com/wiki/ksvc/KSYDiversityLive_Android/images/agoraclass.png" width = "559.5" height = "433" alt="图片名称" align=center />

1 在Build.gradle中添加推流库依赖
>需添加jcenter依赖，参考[demo](https://github.com/ksvc/KSYDiversityLive_Android/blob/master/Agora/build.gradle)

```java
    compile 'com.ksyun.media:libksylive-java:2.2.5'
    compile 'com.ksyun.media:libksylive-arm64:2.2.5'
    compile 'com.ksyun.media:libksylive-armv7a:2.2.5'
    compile 'com.ksyun.media:libksylive-x86:2.2.5'
```

2 集成[JNI](https://github.com/ksvc/KSYDiversityLive_Android/tree/master/Agora/demo/src/main/jni)代码
* 本层代码集成了Agora的Native接口，负责接收remote音视频，并接收本地采集的音频数据
* JNI集成方式相信你不是问题，可自行百度
* 需要修改[jni_RemoteDataObserver.h](https://github.com/ksvc/KSYDiversityLive_Android/blob/master/Agora/demo/src/main/jni/jni_RemoteDataObserver.h)和[jni_RemoteDataObserver.cpp](https://github.com/ksvc/KSYDiversityLive_Android/blob/master/Agora/demo/src/main/jni/jni_RemoteDataObserver.cpp)的包名为你本身的包名

* 在[Android.mk](https://github.com/ksvc/KSYDiversityLive_Android/blob/master/Agora/demo/src/main/jni/Android.mk)中有对[libs](https://github.com/ksvc/KSYDiversityLive_Android/tree/master/Agora/demo/libs)下面的libHDACEngine.so等的依赖，如果您修改libs目录，这里的目录也需要修改  

* 在[Android.mk](https://github.com/ksvc/KSYDiversityLive_Android/blob/master/Agora/demo/src/main/jni/Android.mk)中有对[libs](https://github.com/ksvc/KSYDiversityLive_Android/tree/master/Agora/demo/libs)下面的libHDACEngine.so等的依赖，如果您修改libs目录，这里的目录也需要修改  

3 集成[java](https://github.com/ksvc/KSYDiversityLive_Android/tree/master/Agora/demo/src/main/java/com/ksyun/media/diversity/agorastreamer/agora)代码  

4 更新声网SDK  
您可随时关注[声网的发版信息](https://docs.agora.io/cn/user_guide/communication/Agora_Native_SDK_Release_Notes.html#)，自行更新声网的SDK，SDK更新方式如下：
* 1:下载[声网视频通话Android版SDK](https://www.agora.io/cn/news/download/)和[声网美颜组件](https://www.agora.io/cn/news/download/)
* 2:替换对应的so和jar到[demo/libs](https://github.com/ksvc/KSYDiversityLive_Android/tree/master/Agora/demo/libs)
* 3:替换include下面的两个.h文件到demo的[jni/include](https://github.com/ksvc/KSYDiversityLive_Android/tree/master/Agora/demo/src/main/jni/include)目录下面
           
## 五. 反馈与建议
- 主页：[金山云](http://www.ksyun.com/)
- 邮箱：<zengfanping@kingsoft.com>
- QQ讨论群：574179720
- Issues: <https://github.com/ksvc/KSYDiversityLive_Android/issues>
