package com.smartfoxitsolutions.lockup;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by RAAJA on 23-02-2017.
 */

public class FaqFragment extends Fragment {

    ExpandableListView faqExpandList;
    FaqActivity activity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View parent = inflater.inflate(R.layout.faq_fragment,container,false);
        faqExpandList = (ExpandableListView) parent.findViewById(R.id.faq_fragment_list);
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (FaqActivity) getActivity();
    }

    @Override
    public void onStart() {
        super.onStart();
        String[] faqQuestions = getResources().getStringArray(R.array.tips_faq_headers_list);
        String[] faqTips = getResources().getStringArray(R.array.tips_faq_tips_list);
        String[] faqAnswers = getResources().getStringArray(R.array.tips_faq_answers_list);

        ArrayList<LinkedHashMap<String,String>> questionList = new ArrayList<>();
        for(String question:faqQuestions){
            LinkedHashMap<String,String> questionLinkedList = new LinkedHashMap<>();
            questionLinkedList.put("QUESTION_COLUMN",question);
            questionList.add(questionLinkedList);
        }

        ArrayList<ArrayList<LinkedHashMap<String,String>>> answersMainList = new ArrayList<>();
        ArrayList<LinkedHashMap<String,String>> answersTipsList = new ArrayList<>();
        for(String tips: faqTips){
            LinkedHashMap<String,String> tipsMap = new LinkedHashMap<>();
            tipsMap.put("ANSWER_COLUMN",tips);
            answersTipsList.add(tipsMap);
        }
        answersMainList.add(answersTipsList);
        for (final String answers:faqAnswers){
            ArrayList<LinkedHashMap<String,String>> answersSubList = new ArrayList<LinkedHashMap<String, String>>(){{
                add(new LinkedHashMap<String, String>(){{
                    put("ANSWER_COLUMN",answers);
                }});
            }};
            /*LinkedHashMap<String,String> answersMap = new LinkedHashMap<>();
            answersMap.put("ANSWER_COLUMN",answers);
            answersSubList.add(answersMap);*/
            answersMainList.add(answersSubList);
        }

        SimpleExpandableListAdapter expandableListAdapter = new SimpleExpandableListAdapter(activity
        ,questionList,R.layout.faq_fragment_group_item,new String[]{"QUESTION_COLUMN"},
                new int[]{android.R.id.text1},answersMainList,R.layout.faq_fragment_child_item,
                new String[]{"ANSWER_COLUMN"},new int[]{android.R.id.text1});
        faqExpandList.setAdapter(expandableListAdapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        activity =null;
    }
}
