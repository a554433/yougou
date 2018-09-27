package com.pinyougou.search.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.GroupOptions;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleHighlightQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.GroupEntry;
import org.springframework.data.solr.core.query.result.GroupPage;
import org.springframework.data.solr.core.query.result.GroupResult;
import org.springframework.data.solr.core.query.result.HighlightEntry;
import org.springframework.data.solr.core.query.result.HighlightEntry.Highlight;
import org.springframework.data.solr.core.query.result.HighlightPage;
import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;

@Service(timeout = 5000)
public class ItemSearchServiceImpl implements ItemSearchService {
	@Autowired
	private SolrTemplate solrTemplate;
	@Autowired
	private RedisTemplate redisTemplate;

	@Override
	public Map search(Map searchMap) {
		Map map = new HashMap<>();

		// 关键字空格处理
		String keywords = (String) searchMap.get("keywords");
		searchMap.put("keywords", keywords.replace(" ", ""));
		// 查詢列表
		map.putAll(searchList(searchMap));
		// 分组查询
		List<String> categoryList = searchCategoryList(searchMap);
		map.put("categoryList", categoryList);
		// 查询品牌和规格列表

		String categoryName = (String) searchMap.get("category");
		if ("".equals(categoryName)) {

			if (categoryList.size() > 0) {
				map.putAll(searchBrandAndSpecList(categoryList.get(0)));
			}

		} else {
			map.putAll(searchBrandAndSpecList(categoryName));
		}

		return map;
	}

	// 高亮显示******************
	private Map searchList(Map searchMap) {

		Map map = new HashMap<>();
		HighlightQuery query = new SimpleHighlightQuery();
		HighlightOptions highlightOptions = new HighlightOptions().addField("item_title");// 在哪一列增加高亮

		highlightOptions.setSimplePrefix("<em style='color:red'>");// 前缀
		highlightOptions.setSimplePostfix("</em>");// 后缀
		query.setHighlightOptions(highlightOptions);

		// 关键字查询
		Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
		query.addCriteria(criteria);

		// 按分类筛选......
		if (!"".equals(searchMap.get("category"))) {
			FilterQuery filterQuery = new SimpleFilterQuery();
			Criteria filterCriteria = new Criteria("item_category").is(searchMap.get("category"));
			filterQuery.addCriteria(filterCriteria);
			query.addFilterQuery(filterQuery);
		}
		// 按品牌过滤......
		if (!"".equals(searchMap.get("brand"))) {
			FilterQuery filterQuery = new SimpleFilterQuery();
			Criteria filterCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
			filterQuery.addCriteria(filterCriteria);
			query.addFilterQuery(filterQuery);

		}
		// 过滤规格........
		if (!"".equals(searchMap.get("spec"))) {
			Map<String, String> specMap = (Map) searchMap.get("spec");
			for (String key : specMap.keySet()) {
				FilterQuery filterQuery = new SimpleFilterQuery();

				Criteria filterCriteria = new Criteria("item_spec_" + key).is(specMap.get(key));
				filterQuery.addCriteria(filterCriteria);
				query.addFilterQuery(filterQuery);
			}
		}
		// 按价格筛选.....
		if (!"".equals(searchMap.get("price"))) {

			String priceStr = (String) searchMap.get("price");
			String[] price = priceStr.split("-");
			if (!price[0].equals("0")) {// 如果区间起点不等于 0
				FilterQuery filterQuery = new SimpleFilterQuery();
				Criteria filterCriteria = new Criteria("item_price").greaterThanEqual(price[0]);
				filterQuery.addCriteria(filterCriteria);
				query.addFilterQuery(filterQuery);
			}
			if (!price[1].equals("*")) {// 如果区间终点不等于*
				FilterQuery filterQuery = new SimpleFilterQuery();
				Criteria filterCriteria = new Criteria("item_price").lessThanEqual(price[1]);
				filterQuery.addCriteria(filterCriteria);
				query.addFilterQuery(filterQuery);
			}
		}
		// 分页查询......
		Integer pageNo = (Integer) searchMap.get("pageNo");// 当前页码
		if (pageNo == null) {
			pageNo = 1;
		}
		Integer pageSize = (Integer) searchMap.get("pageSize");// 每页记录数
		if (pageSize == null) {
			pageSize = 20;
		}
		query.setOffset((pageNo - 1) * pageSize);// 从第几条记录查询
		query.setRows(pageSize);
		// 排序...........
		String sortValue = (String) searchMap.get("sort");// ASC DESC
		String sortField = (String) searchMap.get("sortField");// 排序字段
		if (sortValue != null && !sortValue.equals("")) {
			if (sortValue.equals("ASC")) {
				Sort sort = new Sort(Sort.Direction.ASC, "item_" + sortField);
				query.addSort(sort);
			}
			if (sortValue.equals("DESC")) {
				Sort sort = new Sort(Sort.Direction.DESC, "item_" + sortField);
				query.addSort(sort);
			}
		}

		// 获取高亮结果集************
		HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
		List<HighlightEntry<TbItem>> entryList = page.getHighlighted();// 高亮入口
		for (HighlightEntry<TbItem> entry : entryList) {
			List<Highlight> highlights = entry.getHighlights();

			if (highlights.size() > 0 && highlights.get(0).getSnipplets().size() > 0) {
				TbItem item = entry.getEntity();// 获取实体
				item.setTitle(highlights.get(0).getSnipplets().get(0));
			}
		}
		map.put("rows", page.getContent());
		map.put("totalPages", page.getTotalPages());// 总页数
		map.put("total", page.getTotalElements());// 总记录数
		return map;
	}

	// 分組查詢商品列表
	private List searchCategoryList(Map searchMap) {
		List list = new ArrayList<>();
		// 关键词查询
		Query query = new SimpleQuery("*:*");
		Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
		query.addCriteria(criteria);
		// 设置分页选项
		GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");
		query.setGroupOptions(groupOptions);
		// 获取分组页
		GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
		// 获取分组结果
		GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
		// 获取分组入口页
		Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
		List<GroupEntry<TbItem>> entryList = groupEntries.getContent();
		for (GroupEntry<TbItem> entry : entryList) {
			list.add(entry.getGroupValue());
		}
		return list;
	}

	// 查询品牌和规格列表
	private Map searchBrandAndSpecList(String category) {
		Map map = new HashMap();
		// 获取模板id
		Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(category);
		if (typeId != null) {
			// 根据模板id查询品牌列表
			List brandList = (List) redisTemplate.boundHashOps("brandList").get(typeId);
			map.put("brandList", brandList);// 返回值添加品牌列表
			// 根据模板 ID 查询规格列表
			List specList = (List) redisTemplate.boundHashOps("specList").get(typeId);
			map.put("specList", specList);

		}
		return map;
	}

	@Override
	public void importList(List list) {
		solrTemplate.saveBeans(list);
		solrTemplate.commit();
	}

	@Override
	public void deleteByGoodsId(List goodsId) {
		System.out.println("删除商品 ID"+goodsId);
	    Query query=new SimpleQuery();
	    Criteria criteria=new Criteria("item_goodsid").in(goodsId);
		query.addCriteria(criteria);
	    solrTemplate.delete(query);
	    solrTemplate.commit();
		
	}

}
