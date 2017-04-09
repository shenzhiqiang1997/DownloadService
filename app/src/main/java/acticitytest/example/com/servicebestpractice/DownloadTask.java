package acticitytest.example.com.servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by sus on 2017/4/9.
 */

public class DownloadTask extends AsyncTask<String,Integer,Integer> {

    private static final int TYPE_SUCCESS=0;
    private static final int TYPE_FAILED=1;
    private static final int TYPE_PAUSED=2;
    private static final int TYPE_CANCELED=3;

    private int lastProgress;

    private DownloadListener downloadListener;

    private boolean isPause=false;
    private boolean isCancel=false;

    DownloadTask(DownloadListener downloadListener){
        this.downloadListener=downloadListener;
    }


    @Override
    protected Integer doInBackground(String... params) {
        InputStream inputStream=null;
        RandomAccessFile savedFile=null;
        File file=null;
        try {
            String downloadUrl=params[0];
            String fileName=downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file=new File(directory+fileName);
            long downloadedLength=0;
            if (file.exists()){
                downloadedLength=file.length();
            }
            long contentLength=getContentLength(downloadUrl);
            if (contentLength==0){
                return TYPE_FAILED;
            }else if (contentLength==downloadedLength){
                return TYPE_SUCCESS;
            }
            OkHttpClient okHttpClient=new OkHttpClient();
            Request request=new Request.Builder()
                    .url(downloadUrl)
                    .addHeader("RANGE","bytes="+downloadedLength+"-")
                    .build();
            Response response=okHttpClient.newCall(request).execute();
            if (response!=null){
                inputStream=response.body().byteStream();
                savedFile=new RandomAccessFile(file,"rw");
                savedFile.seek(downloadedLength);
                byte[] b=new byte[1024];
                int total=0;
                int len;
                while ((len=inputStream.read(b))!=-1){
                    if (isPause){
                        return TYPE_PAUSED;
                    }else if (isCancel){
                        return TYPE_CANCELED;
                    }else {
                        total+=len;
                    }
                    savedFile.write(b,0,len);
                    int progress= (int) ((total+downloadedLength)*100/contentLength);
                    publishProgress(progress);
                }
                response.body().close();
                return TYPE_SUCCESS;
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if (inputStream!=null){
                    inputStream.close();
                }
                if (savedFile!=null){
                    savedFile.close();
                }
                if (isCancel&&file.exists()){
                    file.delete();
                }
            }catch (Exception e){
                    e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress=values[0];
        if (progress>lastProgress){
            downloadListener.onProgress(progress);
            lastProgress=progress;
        }
    }

    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer){
            case TYPE_SUCCESS:
                downloadListener.onSuccess();
                break;
            case TYPE_FAILED:
                downloadListener.onFailed();
                break;
            case TYPE_PAUSED:
                downloadListener.onPaused();
                break;
            case TYPE_CANCELED:
                downloadListener.onCanceled();
                break;
        }
    }
    public void pauseDownload(){
        this.isPause=true;
    }
    public void cancelDownload(){
        this.isCancel=true;
    }

    private long getContentLength(String downloadUri) throws IOException {
        if (downloadUri!=null){
            OkHttpClient okHttpClient=new OkHttpClient();
            Request request=new Request.Builder()
                    .url(downloadUri)
                    .build();
            Response response=okHttpClient.newCall(request).execute();
            if (response!=null&&response.isSuccessful()){
                long contentLength=response.body().contentLength();
                return contentLength;
            }
        }
        return 0;
    }
}
