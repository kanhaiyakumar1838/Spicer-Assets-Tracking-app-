package com.xlzn.hcpda.uhf.ui.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.List;

public class FragmentPagerAdapter extends FragmentStatePagerAdapter {

    private List<Fragment> datas;
    private List<String> titles;
    //有参
    public FragmentPagerAdapter(@NonNull FragmentManager fm, List<Fragment> datas, List<String> titles) {
        super(fm);
        this.datas = datas;
        this.titles = titles;
    }
    //返回fragment下标
    @NonNull
    @Override
    public Fragment getItem(int position) {
        return datas.get(position);
    }
    //返回fragment个数
    @Override
    public int getCount() {
        return  datas.size();
    }
    //手打pagetitle 返回titles个数
    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return titles.get(position);
    }
}

