package com.pinyougou.solrutil;


import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSON;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbItemExample;
import com.pinyougou.pojo.TbItemExample.Criteria;
//导入商品数据
@Component
public class SolrUtil {
	@Autowired
	private TbItemMapper itemMapper;
	@Autowired
	private SolrTemplate solrTemplate;
	public void  importItemData() {
		TbItemExample example=new TbItemExample();
		Criteria criteria = example.createCriteria();
		criteria.andStatusEqualTo("1");//已審核
		List<TbItem> itemList = itemMapper.selectByExample(example);
		for (TbItem item : itemList) {
			////将 spec 字段中的 json 字符串转换为 map
			Map specMap = JSON.parseObject(item.getSpec());
			item.setSpecMap(specMap);;//给带注解的字段赋值
			System.out.println(item.getBrand());
			System.out.println(item.getSpec());
		}
		solrTemplate.saveBeans(itemList);
		solrTemplate.commit();
	}
	public static void main(String[] args) {
		ApplicationContext context=new ClassPathXmlApplicationContext("classpath*:spring/applicationContext*.xml");
		SolrUtil solrUtil=(SolrUtil) context.getBean("solrUtil");
		solrUtil.importItemData();
	}
  }

