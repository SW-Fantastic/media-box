# MediaBox

基于FFMPEG的音视频录制软件，主要用于Windows，MacOS应该也可以（尚未测试），Linux系统暂时
不支持。

目前是一个正在开发中的项目，功能不多，UI只支持中文，后续会加入其他的音视频处理功能，这个项目也算
是我第一次接触音视频开发的记录。

基本上可以使用了，在Windows的各个功能是最完善的，自带对应的ffmpeg-recorder，需要通过管理员权限安装，
能够录制桌面和音频，但是只能录制一个视频源，不能录制桌面的时候同时录制摄像头。

MacOS支持基于AVFoundation的视频录制，Linux需要使用X11Grab，且不支持音频录制（ffmpeg当前版本尚不支持Wayland）。