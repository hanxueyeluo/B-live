package com.easylive.utils;

import com.easylive.entity.config.AppConfig;
import com.easylive.entity.constants.Constants;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;


@Component
public class FFmpegUtils {
    @Resource
    private AppConfig appConfig;


    //获取缩略图
    public void createImageThumbnail(String filePath){
        String CMD="ffmpeg -i \"%s\" -vf scale=200:-1 \"%s\"";
        CMD=String.format(CMD,filePath,filePath+ Constants.IMAGE_THUMBNAIL_SUFFIX);
        ProcessUtils.executeCommand(CMD,appConfig.getShowFFmepgLog());
    }


    //获取播放时长
    public Integer getVideoInfoDuration(String completeVideo){
        final String CMD_GET_CODE="ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 \"%s\"";
        String cmd=String.format(CMD_GET_CODE,completeVideo);
        String result=ProcessUtils.executeCommand(cmd,appConfig.getShowFFmepgLog());
        if (StringTools.isEmpty(result)) {
            return 0;
        }
        result=result.replace("\n","");
        return new BigDecimal(result).intValue();
    }

    //获取文件编码
    public String getVideoCodec(String videoFilePath) {
        final String CMD_GET_CODE = "ffprobe -v error -select_streams v:0 -show_entries stream=codec_name \"%s\"";
        String cmd = String.format(CMD_GET_CODE, videoFilePath);
        String result = ProcessUtils.executeCommand(cmd, appConfig.getShowFFmepgLog());
        result = result.replace("\n", "");
        result = result.substring(result.indexOf("=") + 1);
        String codec = result.substring(0, result.indexOf("["));
        return codec;
    }

    //将源文件转成mp4编码
    public void convertHevc2Mp4(String newFileName,String videoFilePath){
        String CMD_HEVC_264="ffmpeg -i %s -c:v libx264 -crf 20 %s -y";
        String cmd=String.format(CMD_HEVC_264,newFileName,videoFilePath);
        ProcessUtils.executeCommand(cmd,appConfig.getShowFFmepgLog());
    }

}
