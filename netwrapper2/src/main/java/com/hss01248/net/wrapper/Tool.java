package com.hss01248.net.wrapper;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.webkit.MimeTypeMap;

import com.blankj.utilcode.utils.EncryptUtils;
import com.hss01248.net.cache.ACache;
import com.hss01248.net.config.ConfigInfo;
import com.hss01248.net.config.GlobalConfig;
import com.hss01248.net.util.FileUtils;
import com.hss01248.net.util.TextUtils;
import com.litesuits.android.async.SimpleTask;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import okhttp3.ResponseBody;

/**
 * Created by Administrator on 2016/9/21.
 */
public class Tool {

    public static String urlEncode(String string)  {
        String str = "";
        try {
            str=  URLEncoder.encode(string,"UTF-8");
            return str;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return string;
        }
    }

    public static void updateProgress(ConfigInfo info,long progress,long max){
        if(info.loadingDialog instanceof ProgressDialog && info.loadingDialog.isShowing()){
            ProgressDialog dialog = (ProgressDialog) info.loadingDialog;
            dialog.setProgress((int) progress);
            dialog.setMax((int) max);
        }
    }


    public static boolean writeResponseBodyToDisk(ResponseBody body, final ConfigInfo info) {
       // try {
            // todo change the file location/name according to your needs
            File futureStudioIconFile = new File(info.filePath);
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                byte[] fileReader = new byte[4096];
                final long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;
                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);

                long oldTime = 0L;
                while (true) {
                    int read = inputStream.read(fileReader);
                    if (read == -1) {
                        break;
                    }
                    outputStream.write(fileReader, 0, read);
                    fileSizeDownloaded += read;
                    //MyLog.d( "file download: " + fileSizeDownloaded + " of " + fileSize);//todo 控制频率

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - oldTime > 300 || fileSizeDownloaded == fileSizeDownloaded) {//每300ms更新一次进度
                        oldTime = currentTime;
                        final long finalFileSizeDownloaded = fileSizeDownloaded;
                        callbackOnMainThread(new Runnable() {
                            @Override
                            public void run() {

                                    updateProgress(info,finalFileSizeDownloaded,fileSize);
                                info.listener.onProgressChange(finalFileSizeDownloaded,fileSize);

                                if(finalFileSizeDownloaded == fileSize){
                                    info.listener.onSuccess(info.filePath,info.filePath);
                                    Tool.dismiss(info.loadingDialog);


                                    //文件校验
                                    if(info.isVerify){
                                        String str = "";
                                        if(info.verfyByMd5OrShar1){//md5
                                            str = EncryptUtils.encryptMD5File2String(info.filePath);

                                        }else {//sha1
                                            str = EncryptUtils.encryptSHA1ToString(info.filePath);//todo 缺少shar1文件的算法
                                        }
                                        if(str.equalsIgnoreCase(info.verifyStr)){//校验通过
                                            info.listener.onSuccess(info.filePath,info.filePath);
                                            handleMedia(info);
                                        }else {
                                            info.listener.onError("文件下载失败:校验不一致");
                                        }
                                    }else {
                                        info.listener.onSuccess(info.filePath,info.filePath);
                                        handleMedia(info);
                                    }




                                }
                            }
                        });

                    }


                }

                outputStream.flush();
                return true;
            } catch (final IOException e) {
                e.printStackTrace();
                Tool.callbackOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        info.listener.onError(e.getMessage());
                    }
                });

                return false;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        /*} catch (IOException e) {
            e.printStackTrace();
            return false;
        }*/
    }

    private static void handleMedia(ConfigInfo configInfo) {
        if(configInfo.isNotifyMediaCenter){
            FileUtils.refreshMediaCenter(HttpUtil.context,configInfo.filePath);
        }else {
            if(configInfo.isHideFolder){
                FileUtils.hideFile(new File(configInfo.filePath));
            }
        }

        if(configInfo.isOpenAfterSuccess){
            FileUtils.openFile(HttpUtil.context,new File(configInfo.filePath));
        }

    }



    public static void  callbackOnMainThread(Runnable runnable){
        HttpUtil.getMainHandler().post(runnable);
    }

    public static boolean isJsonEmpty(String data){
        if (data== null || "".equals(data) || "[]".equals(data)
                || "{}".equals(data) || "null".equals(data)) {
            return true;
        }
        return false;
    }

   /* public static void addToken(Map map) {
        if (map != null){
            map.put(NetDefaultConfig.TOKEN, NetDefaultConfig.getToken());//每一个请求都传递sessionid
        }else {
            map = new HashMap();
            map.put(NetDefaultConfig.TOKEN, NetDefaultConfig.getToken());//每一个请求都传递sessionid
        }

    }*/

    public static String generateUrlOfGET(ConfigInfo info){
        StringBuilder stringBuilder= new StringBuilder();
        if((!info.url.startsWith("http")) && (!info.url.startsWith("https"))){
            stringBuilder.append(GlobalConfig.get().getBaseUrl());
        }
        stringBuilder.append(info.url);


        String parms = Tool.getKeyValueStr(info.params);
        if(TextUtils.isNotEmpty(parms) || TextUtils.isNotEmpty(info.paramsStr)){
            if(!info.url.contains("?")){
                stringBuilder.append("?");
            }else if(!info.url.endsWith("&")){
                stringBuilder.append("&");
            }
            if(TextUtils.isNotEmpty(parms)){
                stringBuilder.append(parms);
            }

            if(TextUtils.isNotEmpty(info.paramsStr)){
                stringBuilder.append(info.paramsStr);
                if(!info.paramsStr.endsWith("&")){
                    stringBuilder.append("&");
                }
            }

        }

        return stringBuilder.toString();
    }

    public static String getKeyValueStr(Map<String,String> params) {
        StringBuilder stringBuilder = new StringBuilder();

        for(Map.Entry<String,String> param   : params.entrySet()){
            stringBuilder.append(param.getKey())
                    .append("=")
                    .append(param.getValue())
                    .append("&");
        }
        return stringBuilder.toString();
    }


    public static void handleError(Throwable t,ConfigInfo configInfo){
        if(t != null){
            t.printStackTrace();
        }
        dismiss(configInfo.loadingDialog);
        String str = t.toString();
        if(str.contains("timeout")){
            configInfo.listener.onTimeout();
        }else {
            configInfo.listener.onError(str);
        }
    }

    public static boolean isNetworkAvailable() {
        ConnectivityManager connectivity = (ConnectivityManager) HttpUtil.context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected())
            {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED)
                {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }


    public static String appendUrl(String urlTail,boolean isToAppend) {
        String url ;
        if (!isToAppend || urlTail.contains("http:")|| urlTail.contains("https:")){
            url =  urlTail;
        }else {
            url = GlobalConfig.get().getBaseUrl()+  urlTail;
        }

        return url;
    }


    public static String getCacheKey(ConfigInfo configInfo){
        String url = configInfo.url;
        Map<String,String> map = configInfo.params;
        StringBuilder stringBuilder = new StringBuilder(100);
        stringBuilder.append(url);
        int size = map.size();
        Set<Map.Entry<String,String>> set = map.entrySet();
        if (size>0){
            for (Map.Entry<String,String> entry: set){
                stringBuilder.append(entry.getKey()).append(entry.getValue());
            }

        }

        return stringBuilder.toString();

    }


    /**
     *
     * @param startTime 请求刚开始的时间
     * @param configInfo
     * @param runnable 要执行的代码,通常是最终的网络回调
     * @param <T>
     */
    public static <T> void parseInTime(long startTime, final ConfigInfo<T> configInfo, final Runnable runnable) {
        Tool.dismiss(configInfo.loadingDialog);
      /*  long time2 = System.currentTimeMillis();
        long gap = time2 - startTime;
        if (configInfo.isForceMinTime ){
            long minGap = configInfo.minTime <= 0 ? NetDefaultConfig.TIME_MINI : configInfo.minTime;

            if (gap < minGap){
                TimerUtil.doAfter(new TimerTask() {
                    @Override
                    public void run() {
                        Tool.dismiss(configInfo.loadingDialog);
                        runnable.run();
                    }
                },(minGap - gap));
            }else {
                Tool.dismiss(configInfo.loadingDialog);
                runnable.run();
            }

        }else {
            Tool.dismiss(configInfo.loadingDialog);
            runnable.run();
        }*/
    }





    public static void dismiss(Dialog dialog) {
        if(dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

    }





    private static void cacheResponse(final String string, final ConfigInfo configInfo) {
        if (configInfo.shouldCacheResponse && !configInfo.isFromCache && configInfo.cacheTime >0){
            SimpleTask<Void> simple = new SimpleTask<Void>() {

                @Override
                protected Void doInBackground() {
                    ACache.get(HttpUtil.context).put(getCacheKey(configInfo),string, (int) (configInfo.cacheTime));
                    MyLog.d("caching resonse:"+string);
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                }
            };
            simple.execute();
        }
    }




    private static String getSuffix(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return null;
        }
        String fileName = file.getName();
        if (fileName.equals("") || fileName.endsWith(".")) {
            return null;
        }
        int index = fileName.lastIndexOf(".");
        if (index != -1) {
            return fileName.substring(index + 1).toLowerCase(Locale.US);
        } else {
            return null;
        }
    }

    public static String getMimeType(File file){
        String suffix = getSuffix(file);
        if (suffix == null) {
            return "file/*";
        }
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
        if (type != null || !type.isEmpty()) {
            return type;
        }
        return "file/*";
    }

    public static String getMimeType(String fileUrl){


        String suffix = getSuffix(new File(fileUrl));
        if (suffix == null) {
            return "file";
        }
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
        if (type != null || !type.isEmpty()) {
            return type;
        }
        return "file";
    }



    public  static  <E> void parseStandJsonStr(String string, final ConfigInfo<E> configInfo)  {
        if (isJsonEmpty(string)){//先看是否为空

            callbackOnMainThread(new Runnable() {
                @Override
                public void run() {
                    configInfo.listener.onEmpty();
                }
            });

        }else {

            // final BaseNetBean<E> bean = MyJson.parseObject(string,BaseNetBean.class);//如何解析内部的字段?
          /*  Gson gson = new Gson();z这样也不行
            Type objectType = new TypeToken<BaseNetBean<E>>() {}.getType();
            final BaseNetBean<E> bean = gson.fromJson(string,objectType);*/



            JSONObject object = null;
            try {
                object = new JSONObject(string);
            } catch (com.alibaba.fastjson.JSONException e) {
                e.printStackTrace();

                callbackOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        configInfo.listener.onError("json 格式异常");
                    }
                });
                return;
            } catch (org.json.JSONException e) {
                e.printStackTrace();
            }
            String key_data = TextUtils.isEmpty(configInfo.key_data) ? GlobalConfig.get().getStandardJsonKeyData() : configInfo.key_data;
            String key_code = TextUtils.isEmpty(configInfo.key_code) ? GlobalConfig.get().getStandardJsonKeyCode() : configInfo.key_code;
            String key_msg = TextUtils.isEmpty(configInfo.key_msg) ? GlobalConfig.get().getStandardJsonKeyMsg() : configInfo.key_msg;

            final String dataStr = object.optString(key_data);
            final int code = object.optInt(key_code);
            final String msg = object.optString(key_msg);

            final String finalString1 = string;

            parseStandardJsonObj(finalString1,dataStr,code,msg,configInfo);
            //todo 将时间解析放到后面去

        }
    }

    /**
     * 解析标准json的方法

     * @param configInfo

     * @param <E>
     */
    private static <E> void parseStandardJsonObj(final String response, final String data, final int code,

                                                 final String msg, final ConfigInfo<E> configInfo){

        int codeSuccess = configInfo.isCustomCodeSet ? configInfo.code_success : GlobalConfig.get().getCodeSuccess();
        int codeUnFound = configInfo.isCustomCodeSet ? configInfo.code_unFound :  GlobalConfig.get().getCodeUnfound();
        int codeUnlogin = configInfo.isCustomCodeSet ? configInfo.code_unlogin :  GlobalConfig.get().getCodeUnlogin();

        if (code == codeSuccess){
            if (isJsonEmpty(data)){
                callbackOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        if(configInfo.isResponseJsonArray){
                            configInfo.listener.onEmpty();
                        }else {
                            if(configInfo.isSuccessDataEmpty){
                                configInfo.listener.onSuccess(null,TextUtils.isEmpty(msg)? "请求成功!" :msg);
                            }else {
                                configInfo.listener.onError("数据为空");
                            }
                        }
                    }
                });

            }else {
                try{
                    if (data.startsWith("{")){
                        final E bean =  MyJson.parseObject(data,configInfo.clazz);
                        callbackOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                configInfo.listener.onSuccessObj(bean ,response,data,code,msg);
                            }
                        });


                        //cacheResponse(response, configInfo);
                    }else if (data.startsWith("[")){
                        final List<E> beans =  MyJson.parseArray(data,configInfo.clazz);
                        callbackOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                configInfo.listener.onSuccessArr(beans,response,data,code,msg);
                            }
                        });



                        //cacheResponse(response, configInfo);
                    }else {//如果data的值是一个字符串,而不是标准json,那么直接返回
                        if (String.class.equals(configInfo.clazz) ){//此时,E也是String类型.如果有误,会抛出到下面catch里
                            callbackOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    configInfo.listener.onSuccess((E) data,data);
                                }
                            });



                        }else {
                            callbackOnMainThread(new Runnable() {
                                @Override
                                public void run() {
                                    configInfo.listener.onError("不是标准的json数据");
                                }
                            });

                        }
                    }
                }catch (final Exception e){
                    e.printStackTrace();
                    callbackOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            configInfo.listener.onError(e.toString());
                        }
                    });

                    return;
                }
            }
        }else if (code == codeUnFound){
            callbackOnMainThread(new Runnable() {
                @Override
                public void run() {
                    configInfo.listener.onUnFound();
                }
            });

        }else if (code == codeUnlogin){
            /*configInfo.client.autoLogin(new MyNetListener() {
                @Override
                public void onSuccess(Object response, String resonseStr) {
                    configInfo.client.start(configInfo);
                }
                @Override
                public void onError(String error) {
                    super.onError(error);
                     configInfo.listener.onUnlogin();
                }
            });*/
        }else {
            callbackOnMainThread(new Runnable() {
                @Override
                public void run() {
                    configInfo.listener.onCodeError(msg,"",code);
                }
            });

        }
    }

    public static  void parseStringByType(final String string, final ConfigInfo configInfo) {
        switch (configInfo.type){
            case ConfigInfo.TYPE_STRING:
                //缓存
                //cacheResponse(string, configInfo);
                //处理结果
                callbackOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        configInfo.listener.onSuccess(string, string);
                    }
                });

                break;
            case ConfigInfo.TYPE_JSON:
                parseCommonJson(string,configInfo);
                break;
            case ConfigInfo.TYPE_JSON_FORMATTED:
                parseStandJsonStr(string, configInfo);
                break;
        }
    }

    public static void showDialog(Dialog dialog){
        if(dialog!=null && !dialog.isShowing()){
            try {
                dialog.show();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private static <E> void parseCommonJson(final String string, final ConfigInfo<E> configInfo) {
        if (isJsonEmpty(string)){
            callbackOnMainThread(new Runnable() {
                @Override
                public void run() {
                    configInfo.listener.onEmpty();
                }
            });

        }else {
            try{
                if (string.startsWith("{")){
                    final E bean =  MyJson.parseObject(string,configInfo.clazz);
                    callbackOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            configInfo.listener.onSuccessObj(bean ,string,string,0,"");
                        }
                    });

                    //cacheResponse(string, configInfo);
                }else if (string.startsWith("[")){
                    final List<E> beans =  MyJson.parseArray(string,configInfo.clazz);
                    callbackOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            configInfo.listener.onSuccessArr(beans,string,string,0,"");
                        }
                    });

                    //cacheResponse(string, configInfo);
                }else {
                    callbackOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            configInfo.listener.onError("不是标准json格式");
                        }
                    });

                }
            }catch (final Exception e){
                e.printStackTrace();
                callbackOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        configInfo.listener.onError(e.toString());
                    }
                });

            }
        }
    }


}
