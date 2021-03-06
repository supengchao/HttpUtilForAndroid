# HttpUtilForAndroid

## 特点:

第三方隔离:使用过程中不涉及到下一层的库的相关类,全部是

链式调用

不同类型的请求能达到api的隔离

底层提供了okhttp实现

提供默认-全局-单个请求 三个层次的配置功能

提供data-code-msg三个标准字段的json解析和回调,并且可自定义配置三个字段

api设计上结合http协议和android平台特点来实现: loading对话框,进度条显示,文件下载后的打开/扫描到媒体库

[![](https://jitpack.io/v/hss01248/NetWrapper.svg)](https://jitpack.io/#hss01248/NetWrapper)



# update

[点击查看更新日志](https://github.com/hss01248/NetWrapper/blob/master/updatelog.md)

[1.0.1的api说明文档](/README_OLD.MD)



# 全局配置

## 入口:

```

HttpUtil.init(getApplicationContext(),"http://www.qxinli.com:9001/api/")
		//此方法返回GlobalConfig类,此类中所有方法均为链式调用
		.xxxx//
		
```

## GlobalConfig类的可配置项目

```
//发送自定义的userAgent搞事情
setUserAgent(String userAgent)
//缓存策略
setCacheMode(@CacheStrategy.Mode int cacheMode)
//cookie管理
setCookieMode(int cookieMode)
//url前缀
setBaseUrl(String url)
//三字段标准json
setStandardJsonKeys(String key_data, String key_code, String key_msg)
setStandardJsonCodes(int codeSuccess,int codeUnlogin,int codeUnfound)
//超时时间
setConnectTimeout(int connectTimeout)
setReadTimeout(int readTimeout)
setWriteTimeout(int writeTimeout)
//重试
setRetryCount(int retryCount)
//https
addCrtificateAssert(String fileName)
//打开log
openLog(String logTag) 
```

# 几个入口方法

```
public  static <E> StringRequestBuilder<E> requestString(String url) 

public static <E> JsonRequestBuilder<E> requestJson(String url, Class clazz)

public static <E> StandardJsonRequestBuilder<E> reqeustStandardJson(String url, Class<E> clazz)

public static <E> DownloadBuilder<E> download(String url)

public static <E> UploadRequestBuilder<E> upload(String url, String fileDesc, String filePath)
```

# 单个请求的通用配置

## http方法和回调(链式调用的终点)

```
getAsync(MyNetListener<T> listener)
postAsync(MyNetListener<T> listener)
```

## url

> 一般由上方httpUtil的初始化时设置以及入口方法传入.
>
> 如果入口方法中传入的url含有http或者https,则不会拼接初始化设置的baseUrl.

## http请求参数

### 两种设置形式

```
paramsStr(String paramsStr)//将一整个key=value形式或者json形式的字符串设置进来

addParam(String key,String value)//添加参数键值对

addParams(Map<String,String> params)
```

### 两种传输形式

> post请求时,在请求体中,可以key=value&key=value的形式传输,也可以json字符串的形式传输

```
setParamsAsJson()//默认为key=value的形式,调用此方法,改成以json形式传输
```



# http头

```
addHeader(String key,String value)
addHeaders(Map<String,String> headers)
```

### 缓存控制(todo)

```
setCacheMode(int cacheMode)
```

### cookie管理(todo)

```
setCookieMode(int cookieMode) 
```

## 其他



## https

> 客户端对https的处理有很多策略,目前这里实现了两种,第一种是信任所有证书,第二种是读取客户端预置的证书来通信.

```
setIgnoreCertificateVerify()//设置忽略证书校验
```



## UI: loading对话框

只要传入相关信息,自动帮开发者弹出,关闭.

能在dialog取消时,自动取消对应的网络请求

```
showLoadingDialog(Activity activity, String loadingMsg)//内置的ProgressDialog
showLoadingDialog(Dialog loadingDialog)//传入自定义的dialog
```

内置的dialog特点:

outsideCancelable为false,cancelable为true,也就是点击界面阴影不关闭,点击后退键会关闭

# 分类请求的配置

## 普通字符流请求:StringRequestBuilder

### 构造入口

```
HttpUtil.buildStringRequest(url)
```

### 特殊配置: 无

## 响应为json的请求:

> 自动解析,并在回调中直接返回解析好的javabean

```
HttpUtil.buildJsonRequest("version/latestVersion/v1.json",GetCommonJsonBean.class)
```

示例:

```
HttpUtil.buildJsonRequest("version/latestVersion/v1.json",GetCommonJsonBean.class)
        .showLoadingDialog(MainActivityNew.this,"加载中...")
        .callback(new MyNetListener<GetCommonJsonBean>() {
            @Override
            public void onSuccess(GetCommonJsonBean response, String resonseStr) {
                Logger.json(MyJson.toJsonStr(response));
            }
        })
        .getAsync();
```

### json的解析说明

如果是jsonObject,

clazz传入实体类的Class,同时MyNetListener泛型设置为该实体类

如果JsonArray,:

clazz传入数组元素类的Class,同时MyNetListener泛型设置为该实体类,其回调采用

```
onSuccessArr(List<T> response,String resonseStr)
```





## 响应为三字段标准json的请求:

> 根据用户的配置自动解析三个字段,并回调.三个字段和code的几个取值单个请求没有设置的话,采用全局中的设置.



### 解析过程为:

> 服务器返回json后,
>
> 先根据定义的code判断成功还是失败
>
> 如果成功,直接将result对应的jsonObject解析成GetStandardJsonBean(下面代码)

```

//返回json后,先根据定义的code判断成功还是失败,
HttpUtil.buildStandardJsonRequest("http://japi.juhe.cn/joke/content/list.from",GetStandardJsonBean.class)
        .addParam("sort","desc")
        .addParam("page","1")
        .addParam("pagesize","4")
        .addParam("time",System.currentTimeMillis()/1000+"")
        .addParam("key","fuck you")
        .setStandardJsonKey("result","error_code","reason")
        .setCustomCodeValue(0,2,-1)
        .showLoadingDialog(MainActivityNew.this,"老司机开车了...")
        .callback(new MyNetListener<GetStandardJsonBean>() {
            @Override
            public void onSuccess(GetStandardJsonBean response, String resonseStr) {
                Logger.json(MyJson.toJsonStr(response));
            }
            @Override
            public void onError(String error) {
                super.onError(error);
                Logger.e(error);
            }
        })
        .getAsync();
```

## 文件下载

### 提供的自定义配置有:

文件的保存路径,默认是download文件夹下

下载完成后是否校验md5或者sha1,默认不校验,如果校验不成功,则表示下载失败

下载成功后,是否打开文件(不用关系文件类型,内部已经判断,并调用系统intent去打开)

下载成功后,是否通知媒体库(比如下载的图片,音视频,想立即让用户可以在系统相册中看到,就需要通知),默认是通知的

下载成功后,是否隐藏该文件,让系统媒体库扫描不到.(通过在文件所在的文件夹下建立一个名为.nomedia的空文件来实现)

### 对话框特点: 

通过showLoadingDialog弹出的自带下载进度显示,无需开发者再操作.

默认是直线进度条

可以选择是圆圈型的还是直线型的,可采用下面的方法配置

```
 showLoadingDialog(Activity activity, String loadingMsg, boolean updateProgress, boolean horizontal)
```

> 暂不实现多线程下载,断点续传等功能

### 示例

```
HttpUtil.buildDownloadRequest(url2)
        .showLoadingDialog(MainActivityNew.this)//显示下载进度dialog
        .savedPath(path)//自定义保存路径
        .setOpenAfterSuccess()//下载完成后打开
        .setHideFile()//隐藏该文件
        .setNotifyMediaCenter(true)//通知媒体库
        .verifyMd5("djso8d89dsjd9s7dsfj")//下载完后校验md5
        .getAsync(new MyNetListener() {
            @Override
            public void onSuccess(Object response, String onSuccess) {
                Logger.e("onSuccess:"+onSuccess);
            }

            @Override
            public void onProgressChange(long fileSize, long downloadedSize) {
                super.onProgressChange(fileSize, downloadedSize);
                Logger.e("progress:"+downloadedSize+"--filesize:"+fileSize);
            }

            @Override
            public void onError(String msgCanShow) {
                super.onError(msgCanShow);
                Logger.e(msgCanShow);
            }
        });
```

## 文件上传

> 支持多文件上传的进度回调
>
> 暂不实现多线程上传/分片上传/断点续传等高级功能

### 对话框的进度说明

对于开发者来说,也是带进度的对话框,情况和下载的一样.

### 示例

```
HttpUtil.buildUpLoadRequest("http://192.168.1.100:8080/gm/file/q_uploadAndroidApk.do","uploadFile1","/storage/emulated/0/qxinli.apk")//入口方法中至少添加一个文件
        .addFile("uploadFile2","/storage/emulated/0/Download/retrofit/qxinli-2.apk")//添加文件的方法
        .addParam("uploadFile555","1474363536041.jpg")
        .addParam("api_secret777","898767hjk")
        .showLoadingDialog(this)
        .postAsync(new MyNetListener<String>() {
                            @Override
                            public void onSuccess(String response, String resonseStr) {
                                Logger.e(resonseStr);
                            }

                            @Override
                            public void onError(String error) {
                                super.onError(error);
                                Logger.e("error:"+error);
                            }

                            @Override
                            public void onProgressChange(long downloadedSize, long fileSize) {
                                super.onProgressChange(fileSize, downloadedSize);
                                Logger.e("upload onProgressChange:"+downloadedSize + "  total:"+ fileSize +"  progress:"+downloadedSize*100/fileSize);
                            }
                        });
```



# 请求的取消

## 取消单个请求

### 通过tag取消

> 最佳实践: 在activity的onDestory方法中取消该activity中的相关请求

```
HttpUtil.cancelRquest(tag);
```

### 通过dialog取消

如果弹出dialog,那么不管有没有设置tag,只要取消dialog,就能取消该网络请求.

## 取消所有请求

HttpUtil.cancleAllRequest()

# usage

## gradle

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```

Step 2. Add the dependency

```java
dependencies {
        compile 'com.github.hss01248:HttpUtilForAndroid:2.0.0'
}
```



