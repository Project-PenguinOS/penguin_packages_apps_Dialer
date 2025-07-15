/*
 * Copyright (C) 2023 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.dialer.app.calllog;

import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.contacts.common.list.ViewPagerTabs;
import com.android.dialer.R;
import com.android.dialer.database.CallLogQueryHandler;

public class NewCallLogFragment extends Fragment {

    private static final int TAB_INDEX_ALL = 0;
    private static final int TAB_INDEX_MISSED = 1;
    private static final int TAB_INDEX_COUNT = 2;

    private ViewPager viewPager;
    private ViewPagerTabs viewPagerTabs;
    private ViewPagerAdapter viewPagerAdapter;
    private String[] tabTitles;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.new_call_log_fragment, container, false);

        tabTitles = new String[TAB_INDEX_COUNT];
        tabTitles[0] = getString(R.string.call_log_all_title);
        tabTitles[1] = getString(R.string.call_log_missed_title);

        viewPager = view.findViewById(R.id.new_call_log_pager);
        viewPagerAdapter = new ViewPagerAdapter(getChildFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(1);

        viewPagerTabs = view.findViewById(R.id.new_viewpager_header);
        viewPagerTabs.setViewPager(viewPager);

        return view;
    }

    public class ViewPagerAdapter extends FragmentPagerAdapter {

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case TAB_INDEX_ALL:
                    return new CallLogFragment(CallLogQueryHandler.CALL_TYPE_ALL, false);
                case TAB_INDEX_MISSED:
                    return new CallLogFragment(Calls.MISSED_TYPE, false);
                default:
                    throw new IllegalStateException("No fragment at position " + position);
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabTitles[position];
        }

        @Override
        public int getCount() {
            return TAB_INDEX_COUNT;
        }
    }
}
