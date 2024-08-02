package com.xlzn.hcpda.uhf.interfaces;

import com.xlzn.hcpda.uhf.entity.UHFTagEntity;

import java.util.List;

/*
 * 监听盘点回调数据
 */
public interface OnInventoryDataListener {
    public void onInventoryData(List<UHFTagEntity> tagEntityList);
}
