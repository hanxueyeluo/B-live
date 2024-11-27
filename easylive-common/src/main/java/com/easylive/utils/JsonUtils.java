package com.easylive.utils;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jdk.nashorn.internal.runtime.regexp.JoniRegExp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class JsonUtils {
    private static Logger lgger= LoggerFactory.getLogger(JsonUtils.class);

    public static String convertObj2Json(Object obj){
        return JSON.toJSONString(obj);
    }

    public static <T> T convertJson2Obj(String json,Class<T> tClass){
        return JSONObject.parseObject(json,tClass);
    }

    public static <T> List<T> convertJsonArray2List(String json,Class<T> tClass){
        return JSONArray.parseArray(json,tClass);
    }

    public static void main(String[] args) {

    }

}
