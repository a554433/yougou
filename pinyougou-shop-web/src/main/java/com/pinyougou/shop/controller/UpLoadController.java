package com.pinyougou.shop.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import entity.Result;
import util.FastDFSClient;

@RestController
public class UpLoadController {
	
	@Value("${FILE_SERVER_URL}")
	private String file_server_url;

	@RequestMapping("/upload")
	public Result upload(MultipartFile file){
		
		String originalFilename = file.getOriginalFilename();//获取文件名
		String extName=originalFilename.substring( originalFilename.lastIndexOf(".")+1);//得到扩展名
		
		try {
			FastDFSClient fastDFSClient=new FastDFSClient("classpath:config/fdfs_client.conf");
			//3、执行上传处理
			String path = fastDFSClient.uploadFile(file.getBytes(),extName);
			
			//4、拼接返回的 url 和 ip 地址，拼装成完整的 url
			String url=file_server_url+path;
			
			System.out.println(url);
			return new Result(true,url);
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, "上传失败");
		}
		
	}
}
