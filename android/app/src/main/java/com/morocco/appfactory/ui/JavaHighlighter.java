package com.morocco.appfactory.ui;

import android.text.*;
import android.text.style.ForegroundColorSpan;
import java.util.regex.*;

public final class JavaHighlighter implements TextWatcher {
    private static final Pattern TOKENS=Pattern.compile("(//[^\\n]*|/\\*[\\s\\S]*?\\*/)|(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')|\\b(abstract|boolean|break|byte|case|catch|char|class|continue|default|do|double|else|enum|extends|final|finally|float|for|if|implements|import|instanceof|int|interface|long|native|new|null|package|private|protected|public|return|short|static|super|switch|synchronized|this|throw|throws|true|false|try|void|volatile|while)\\b");
    private boolean working;
    private final Runnable changed;
    public JavaHighlighter(Runnable callback){changed=callback;}
    public void beforeTextChanged(CharSequence s,int start,int count,int after){}
    public void onTextChanged(CharSequence s,int start,int before,int count){}
    public void afterTextChanged(Editable text){
        if(working)return;working=true;
        try{for(ForegroundColorSpan span:text.getSpans(0,text.length(),ForegroundColorSpan.class))text.removeSpan(span);
            Matcher matcher=TOKENS.matcher(text);while(matcher.find()){int color=matcher.group(1)!=null?0xFF7E8A90:matcher.group(2)!=null?0xFF98C379:0xFF6BA7FF;text.setSpan(new ForegroundColorSpan(color),matcher.start(),matcher.end(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
        }finally{working=false;}changed.run();
    }
}
