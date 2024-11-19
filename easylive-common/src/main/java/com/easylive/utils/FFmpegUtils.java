package com.easylive.utils;

import com.easylive.entity.config.AppConfig;
import com.easylive.entity.constants.Constants;

import javax.annotation.Resource;

public class FFmpegUtils {
    @Resource
    private AppConfig appConfig;

    public void createImageThumbnail(String filePath){
        String CMD="ffmpeg -i \"%s\" -vf scale=200:-1 \"%s\"";
        CMD=String.format(CMD,filePath,filePath+ Constants.IMAGE_THUMBNAIL_SUFFIX);
        ProcessUtils.executeCommand(CMD,appConfig.getShowFFmepgLog());
    }

}
