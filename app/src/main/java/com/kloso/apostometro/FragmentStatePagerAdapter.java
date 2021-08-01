package com.kloso.apostometro;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.kloso.apostometro.ui.BetsFragment;

import java.util.List;

public class FragmentStatePagerAdapter extends androidx.fragment.app.FragmentPagerAdapter {

    private List<Fragment> fragmentList;


    public FragmentStatePagerAdapter(@NonNull FragmentManager fm, int behavior, List<Fragment> fragmentList) {
        super(fm, behavior);
        this.fragmentList = fragmentList;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return fragmentList.get(position);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        Fragment fragment = fragmentList.get(position);
        return ((BetsFragment) fragment).getTabName();
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }
}
