package com.easylive.component;

import com.easylive.entity.config.AppConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.SysSettingDto;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.dto.UploadFileDto;
import com.easylive.entity.enums.DateTimePatternEnum;
import com.easylive.entity.po.CategoryInfo;
import com.easylive.redis.RedisUtils;
import com.easylive.utils.DateUtil;
import com.easylive.utils.StringTools;
import org.elasticsearch.common.recycler.Recycler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.net.ServerSocket;
import java.util.Date;
import java.util.List;
import java.util.UUID;


@Component
public class RedisComponent {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private AppConfig appConfig;

    public String saveCheckCode(String code){
        String checkCodeKey= UUID.randomUUID().toString();
        redisUtils.setex(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey,code,Constants.REDIS_KEY_EXPIRES_ONE_MIN);
        return checkCodeKey;
    }

    public String getCheckCode(String checkCodeKey){
        return (String) redisUtils.get(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey);
    }

    public void cleanCheckCode(String checkCodeKey){
         redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey);
    }

    public void saveTokenInfo(TokenUserInfoDto tokenUserInfoDto){
        String token=UUID.randomUUID().toString();
        tokenUserInfoDto.setExpireAt(System.currentTimeMillis()+Constants.REDIS_KEY_EXPIRES_ONE_DAY*15);
        tokenUserInfoDto.setToken(token);
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_WEB+token,tokenUserInfoDto,Constants.REDIS_KEY_EXPIRES_ONE_DAY*15);
    }

    public void cleanToken(String token){
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_WEB+token);
    }

    public TokenUserInfoDto getToken(String token){
       return (TokenUserInfoDto) redisUtils.get(Constants.REDIS_KEY_TOKEN_WEB+token);
    }

    public String getForAdminToken(String token){
        return (String) redisUtils.get(Constants.REDIS_KEY_TOKEN_ADMIN+token);
    }

    public String saveToken4Admin(String account){
        String token=UUID.randomUUID().toString();
        redisUtils.setex(Constants.REDIS_KEY_TOKEN_ADMIN+token,account,Constants.REDIS_KEY_EXPIRES_ONE_DAY);
        return token;
    }

    public void cleanToken4Admin(String token){
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_ADMIN+token);
    }

    public void saveCategoryList(List<CategoryInfo> categoryInfoList) {
        redisUtils.set(Constants.REDIS_KEY_CATEGORY_LIST,categoryInfoList);
    }

    public List<CategoryInfo> getCategoryList() {
        return (List<CategoryInfo>) redisUtils.get(Constants.REDIS_KEY_CATEGORY_LIST);
    }

    public String savePreVideoFileInfo(String userId,String fileName,Integer chunks){
        String uploadId= StringTools.getRandomString(Constants.LENGTH_15);
        UploadFileDto fileDto=new UploadFileDto();
        fileDto.setChunks(chunks);
        fileDto.setFileName(fileName);
        fileDto.setUploadId(uploadId);
        fileDto.setChunkIndex(0);
        String day= DateUtil.format(new Date(), DateTimePatternEnum.YYYYMMDD.getPattern());
        String filePath=day+"/"+userId+uploadId;
        String folder=appConfig.getProjectFolder()+Constants.FILE_FOLDER+Constants.FILE_FOLDER_TEMP+filePath;
        File folderFile=new File(folder);
        if (!folderFile.exists()){
            folderFile.mkdirs();
        }
        fileDto.setFilePath(filePath);
        redisUtils.setex(Constants.REDIS_KEY_UPLOADING_FILE+userId+uploadId,fileDto,Constants.REDIS_KEY_EXPIRES_ONE_DAY);
        return uploadId;
    }

    public UploadFileDto getUploadVideoDto(String userId,String uploadId){
        return (UploadFileDto) redisUtils.get(Constants.REDIS_KEY_UPLOADING_FILE+userId+uploadId);
    }

    public SysSettingDto getSysSettingDto(){
        SysSettingDto sysSettingDto= (SysSettingDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if (sysSettingDto == null) {
            sysSettingDto=new SysSettingDto();
        }
        return sysSettingDto;
    }

    public void updateVideoFileInfo(String userId, UploadFileDto uploadFileDto) {
        redisUtils.setex(Constants.REDIS_KEY_UPLOADING_FILE+userId+uploadFileDto.getUploadId(),uploadFileDto,Constants.REDIS_KEY_EXPIRES_ONE_DAY);
    }
}
