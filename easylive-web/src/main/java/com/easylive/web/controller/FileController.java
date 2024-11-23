package com.easylive.web.controller;


import com.easylive.component.RedisComponent;
import com.easylive.entity.config.AppConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.SysSettingDto;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.dto.UploadFileDto;
import com.easylive.entity.enums.DateTimePatternEnum;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.exception.BusinessException;
import com.easylive.utils.DateUtil;
import com.easylive.utils.FFmpegUtils;
import com.easylive.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.util.Date;

@RestController
@RequestMapping("/file")
@Slf4j
@Validated
public class FileController extends ABaseController{
    @Resource
    private AppConfig appConfig;
    @Resource
    private RedisComponent redisComponent;
    @RequestMapping("/getResource")
    public void getResource(HttpServletResponse response, @NotNull String sourceName) throws FileNotFoundException {
        if(!StringTools.pathIsOk(sourceName)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String suffix=StringTools.getFileSuffix(sourceName);
        response.setContentType("image/"+suffix.replace(".",""));
        response.setHeader("Cache-control","max-age=2592000");
        readFile(response,sourceName);
    }

    protected void readFile(HttpServletResponse response,String filePath) throws FileNotFoundException {
        File file=new File(appConfig.getProjectFolder()+Constants.FILE_FOLDER+filePath);
        if (!file.exists()) {
            return;
        }
        try (OutputStream out=response.getOutputStream(); FileInputStream in=new FileInputStream(file)){
            byte[] byteData=new byte[1024];
            int len=0;
            while((len=in.read(byteData))!=-1){
                out.write(byteData,0,len);
            }
            out.flush();
        } catch (Exception e) {
            log.error("读取文件异常",e);
        }
    }

    @RequestMapping("/preUploadVideo")
    public ResponseVO preUploadVideo(@NotEmpty String fileName,@NotNull Integer chunks){
        TokenUserInfoDto tokenUserInfoDto=getTokenUserInfoDto();
        String uploadId=redisComponent.savePreVideoFileInfo(tokenUserInfoDto.getUserId(),fileName,chunks);
        return getSuccessResponseVO(uploadId);
    }

    @RequestMapping("/uploadVideo")
    public ResponseVO uploadVideo(@NotNull MultipartFile chunkFile,@NotNull Integer chunkIndex,@NotEmpty String uploadId) throws IOException {
        TokenUserInfoDto tokenUserInfoDto=getTokenUserInfoDto();
        UploadFileDto uploadFileDto=redisComponent.getUploadVideoDto(tokenUserInfoDto.getUserId(),uploadId);
        if (uploadFileDto==null){
            throw new BusinessException("文件不存在，请重新上传");
        }
        SysSettingDto sysSettingDto=redisComponent.getSysSettingDto();
        if (uploadFileDto.getFileSize()>sysSettingDto.getVideoSize()*Constants.MB_SIZE){
            throw new BusinessException("文件超过发小限制");
        }
        //判断分片
        if((chunkIndex-1)> uploadFileDto.getChunkIndex()||chunkIndex>uploadFileDto.getChunks()-1){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String folder=appConfig.getProjectFolder()+Constants.FILE_FOLDER+Constants.FILE_FOLDER_TEMP+uploadFileDto.getFilePath();
        File targetFile=new File(folder+"/"+chunkIndex);
        chunkFile.transferTo(targetFile);
        uploadFileDto.setChunkIndex(chunkIndex);
        uploadFileDto.setFileSize(uploadFileDto.getFileSize()+chunkFile.getSize());
        redisComponent.updateVideoFileInfo(tokenUserInfoDto.getUserId(),uploadFileDto);
        return getSuccessResponseVO(null);
    }


}
