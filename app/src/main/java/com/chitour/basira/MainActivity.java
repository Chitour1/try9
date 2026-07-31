package com.chitour.basira;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private final Engine engine = new Engine();
    private LinearLayout messages;
    private ScrollView scroll;
    private EditText input;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(18,59,42));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(244,241,232));

        TextView header = text("بصيرة", 24, Color.WHITE, true);
        header.setGravity(Gravity.CENTER);
        header.setPadding(20, 28, 20, 28);
        header.setBackgroundColor(Color.rgb(18,59,42));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        TextView notice = text("مساعد إسلامي محلي محدود • بلا إنترنت • ليس جهة فتوى", 13, Color.rgb(70,70,70), false);
        notice.setGravity(Gravity.CENTER);
        notice.setPadding(18, 14, 18, 14);
        root.addView(notice, new LinearLayout.LayoutParams(-1, -2));

        scroll = new ScrollView(this);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(14, 8, 14, 8);
        scroll.addView(messages, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(10, 10, 10, 14);

        input = new EditText(this);
        input.setHint("اكتب سؤالك هنا...");
        input.setTextDirection(View.TEXT_DIRECTION_RTL);
        input.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        input.setSingleLine(false);
        input.setMaxLines(4);
        input.setPadding(18, 12, 18, 12);
        input.setBackground(round(Color.WHITE, 20, Color.rgb(205,197,178)));
        composer.addView(input, new LinearLayout.LayoutParams(0, -2, 1f));

        Button send = new Button(this);
        send.setText("إرسال");
        send.setTextColor(Color.WHITE);
        send.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        send.setBackground(round(Color.rgb(18,59,42), 20, Color.rgb(18,59,42)));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2);
        bp.setMargins(8,0,0,0);
        composer.addView(send, bp);
        root.addView(composer, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
        addBubble("السلام عليكم. أنا بصيرة، مساعد محلي للأساسيات الإسلامية. اسألني مثلًا: ما أركان الإسلام؟ كيف أتوضأ؟ ما شروط التوبة؟", false);
        send.setOnClickListener(v -> send());
    }

    private void send() {
        String q = input.getText().toString().trim();
        if (TextUtils.isEmpty(q)) return;
        addBubble(q, true);
        input.setText("");
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);
        addBubble(engine.answer(q), false);
    }

    private void addBubble(String value, boolean mine) {
        TextView bubble = text(value, 16, mine ? Color.WHITE : Color.rgb(35,35,35), false);
        bubble.setTextDirection(View.TEXT_DIRECTION_RTL);
        bubble.setGravity(Gravity.RIGHT);
        bubble.setPadding(18, 14, 18, 14);
        bubble.setBackground(round(mine ? Color.rgb(18,59,42) : Color.WHITE, 22, mine ? Color.rgb(18,59,42) : Color.rgb(218,210,194)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.gravity = mine ? Gravity.LEFT : Gravity.RIGHT;
        p.setMargins(mine ? 52 : 6, 7, mine ? 6 : 52, 7);
        messages.addView(bubble, p);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable round(int fill, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill); d.setCornerRadius(radius); d.setStroke(1, stroke); return d;
    }

    static final class Engine {
        static final class E {
            final String title, answer, source; final String[] keys; final Set<String> tokens;
            E(String t, String a, String s, String... k) {
                title=t; answer=a; source=s; keys=k;
                tokens = tokenSet(normalize(t + " " + String.join(" ", k)));
            }
            String out(){ return answer + "\n\nالمصدر المختصر: " + source; }
        }
        private final List<E> data = new ArrayList<>();
        private E last;
        private static final Pattern DIAC = Pattern.compile("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]");
        private static final Pattern NON = Pattern.compile("[^\\p{L}\\p{N}]+");
        private static final Set<String> STOP = new HashSet<>(Arrays.asList("ما","هو","هي","من","في","على","الى","عن","هل","كم","كيف","هذا","هذه","ثم"));

        Engine(){
            add("أركان الإسلام","أركان الإسلام خمسة: شهادة أن لا إله إلا الله وأن محمدًا رسول الله، وإقام الصلاة، وإيتاء الزكاة، وصوم رمضان، وحج البيت لمن استطاع إليه سبيلًا.","صحيح البخاري وصحيح مسلم: حديث ابن عمر.","اركان الاسلام","الشهادتان","الصلاه","الزكاه","الصيام","الحج");
            add("أركان الإيمان","أركان الإيمان ستة: الإيمان بالله، وملائكته، وكتبه، ورسله، واليوم الآخر، والقدر خيره وشره.","صحيح مسلم: حديث جبريل.","اركان الايمان","الله","الملائكه","الكتب","الرسل","اليوم الاخر","القدر");
            add("التوحيد","التوحيد هو إفراد الله بالعبادة، والإيمان بأنه وحده الخالق المدبر، وإثبات ما أثبته لنفسه من الأسماء والصفات بلا تحريف ولا تمثيل.","القرآن: الفاتحة 5، والإخلاص.","التوحيد","عباده الله وحده","لا اله الا الله");
            add("الوضوء","صفة الوضوء المختصرة: النية بالقلب، ثم التسمية، وغسل الكفين، والمضمضة والاستنشاق، وغسل الوجه، ثم اليدين إلى المرفقين، ومسح الرأس مع الأذنين، وغسل الرجلين إلى الكعبين، مع الترتيب.","القرآن: المائدة 6، وأحاديث صفة الوضوء في الصحيحين.","الوضوء","اتوضا","غسل الوجه","مسح الراس","غسل الرجلين");
            add("نواقض الوضوء","من النواقض المتفق على أصلها: ما خرج من السبيلين كالريح والبول والغائط، وزوال العقل بإغماء أو سكر، والنوم المستغرق. توجد تفاصيل فقهية أخرى.","القرآن: المائدة 6، وأحاديث الحدث.","نواقض الوضوء","يبطل الوضوء","الريح","البول","الغائط","النوم");
            add("الصلوات المفروضة","الصلوات المفروضة خمس: الفجر، والظهر، والعصر، والمغرب، والعشاء.","الصحيحان: أحاديث فرض الصلوات الخمس.","الصلوات الخمس","عدد الصلوات","الفجر","الظهر","العصر","المغرب","العشاء");
            add("ركعات الفرائض","ركعات الفرائض للمقيم: الفجر ركعتان، والظهر أربع، والعصر أربع، والمغرب ثلاث، والعشاء أربع.","عمل المسلمين المتواتر وأحاديث صفة الصلاة.","ركعات","الفجر ركعتان","الظهر اربع","العصر اربع","المغرب ثلاث","العشاء اربع");
            add("شروط الصلاة","من شروط الصلاة الأساسية: دخول الوقت، والطهارة، وستر العورة، واستقبال القبلة للقادر، والنية بالقلب.","القرآن: النساء 103، المائدة 6، البقرة 144، الأعراف 31.","شروط الصلاه","الطهاره","الوقت","العوره","القبله","النيه");
            add("القبلة","قبلة المسلمين هي الكعبة في المسجد الحرام بمكة، ويتوجه المصلي إلى جهتها بحسب استطاعته.","القرآن: البقرة 144.","القبله","الكعبه","المسجد الحرام","مكه");
            add("صيام رمضان","صيام رمضان هو الإمساك بنية العبادة عن المفطرات من طلوع الفجر الصادق إلى غروب الشمس، وهو فرض على المسلم المكلف القادر.","القرآن: البقرة 183-185.","رمضان","الصيام","الفجر","المغرب","الامساك");
            add("مفطرات واضحة","من المفطرات الواضحة: الأكل والشرب عمدًا، والجماع نهار رمضان. أما الأدوية والمرض والتفاصيل الشخصية فتحتاج فتوى موثوقة.","القرآن: البقرة 187.","يفطر","مبطلات الصيام","الاكل","الشرب","الجماع");
            add("الزكاة","الزكاة حق واجب في أموال مخصوصة بشروط. وفي النقود وعروض التجارة تكون في الجملة 2.5% بعد بلوغ النصاب ومرور حول قمري، لكن الحساب التفصيلي يحتاج معرفة حالتك.","القرآن: البقرة 43 والتوبة 60.","الزكاه","النصاب","الحول","اثنان ونصف");
            add("الحج","الحج قصد بيت الله الحرام لأداء المناسك في زمنها، وهو واجب مرة واحدة في العمر على المسلم المستطيع.","القرآن: آل عمران 97.","الحج","الاستطاعه","مكه","البيت");
            add("التوبة","التوبة الصادقة تكون بالإقلاع عن الذنب، والندم عليه، والعزم على عدم العودة، ورد حقوق الناس إن تعلق الذنب بهم. ولا تيأس من رحمة الله.","القرآن: الزمر 53، والتحريم 8.","التوبه","الندم","الاقلاع","العزم","رد الحقوق");
            add("بر الوالدين","بر الوالدين من أعظم الواجبات: بالإحسان إليهما، ولين الكلام، والخدمة، والدعاء، وطاعتهما في المعروف، وعدم إيذائهما.","القرآن: الإسراء 23-24.","بر الوالدين","الام","الاب","الاحسان");
            add("الغيبة","الغيبة أن تذكر المسلم في غيبته بما يكره، ولو كان صحيحًا. وإن كان كذبًا فهو بهتان.","القرآن: الحجرات 12، وصحيح مسلم.","الغيبه","ذكرك اخاك","يكره","البهتان");
            add("آداب الطعام","من آداب الطعام: أن تقول بسم الله، وتأكل بيمينك، وتأكل مما يليك، ولا تعيب الطعام، وتحمد الله بعده، وتتجنب الإسراف.","الصحيحان: حديث سم الله وكل بيمينك.","الطعام","بسم الله","اليمين","الحمد لله");
            add("الدعاء","ادع الله وحده بإخلاص وحضور قلب، وابدأ بحمده والصلاة على النبي ﷺ، ولا تستعجل الإجابة ولا تدع بإثم أو قطيعة رحم.","القرآن: غافر 60، والبقرة 186.","الدعاء","ادعوني","الاخلاص","الاستجابه");
        }

        String answer(String raw){
            String q=normalize(raw);
            if(q.isEmpty()) return "اكتب سؤالك أولًا.";
            if(has(q,"السلام عليكم","سلام عليكم","مرحبا","اهلا")) return "وعليكم السلام ورحمة الله وبركاته. اسألني في أساسيات الإسلام والعبادات والأخلاق.";
            if(has(q,"شكرا","جزاك الله","بارك الله فيك")) return "وإياك، بارك الله فيك.";
            if(has(q,"من انت","ما اسمك")) return "أنا بصيرة: مساعد إسلامي محلي صغير يعمل بلا إنترنت ولا نموذج خارجي. معرفتي محدودة وليست فتوى.";
            if(has(q,"طلاق","ميراث","تكفير","فتوى","قرض","تمويل","اسهم","تداول","حائض","نفاس","دواء","مرض","حامل","رضاعه")) return "هذه مسألة تحتاج عالمًا مؤهلًا يعرف تفاصيل الواقعة. أنا مساعد تعليمي محلي للأساسيات ولست جهة فتوى.";
            if(last!=null && q.length()<35 && has(q,"اشرح","وضح","ما الدليل","كم عددها","اذكرها","بالتفصيل")) return last.out();
            E best=null; double score=0;
            Set<String> qt=tokenSet(q);
            for(E e:data){
                int ov=0; for(String t:qt) if(e.tokens.contains(t)) ov++;
                double s=qt.isEmpty()?0:(double)ov/qt.size();
                for(String k:e.keys) if(q.contains(normalize(k))) s+=0.35;
                if(s>score){score=s; best=e;}
            }
            if(best!=null && score>=0.45){ last=best; return best.out(); }
            return "لم أجد جوابًا موثوقًا داخل معرفتي المحدودة. جرّب سؤالًا مباشرًا مثل: ما أركان الإيمان؟ كيف أتوضأ؟ كم عدد الصلوات؟ ما شروط التوبة؟";
        }

        private void add(String t,String a,String s,String...k){data.add(new E(t,a,s,k));}
        private static boolean has(String q,String...p){for(String x:p)if(q.contains(normalize(x)))return true;return false;}
        private static Set<String> tokenSet(String q){Set<String>s=new HashSet<>();for(String t:q.split("\\s+"))if(t.length()>1&&!STOP.contains(t))s.add(t);return s;}
        private static String normalize(String v){
            String x=Normalizer.normalize(v==null?"":v,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
            x=DIAC.matcher(x).replaceAll("").replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي').replace('ؤ','و').replace('ئ','ي').replace('ة','ه').replace('ـ',' ');
            return NON.matcher(x).replaceAll(" ").trim().replaceAll("\\s+"," ");
        }
    }
}
