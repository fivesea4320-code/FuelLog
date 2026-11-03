package com.example.fuellog;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import java.text.*;
import java.util.*;
import java.io.*;

public class MainActivity extends Activity {
    private static final int EXPORT_CSV=44;
    private Db db;
    private LinearLayout root, entries;
    private TextView headline, kmPerLiter, costPerKm, details, tips;
    private EditText odoInput, literInput, priceInput;
    private CheckBox fullInput;
    private Button dateButton;
    private String chosenDate;
    private final Locale ko=Locale.KOREA;
    private final DecimalFormat one=new DecimalFormat("#,##0.0");
    private final DecimalFormat money=new DecimalFormat("#,##0");

    static class Fuel {
        long id; String date; double odo, liters, cost; boolean full;
        Fuel(long id,String date,double odo,double liters,double cost,boolean full){this.id=id;this.date=date;this.odo=odo;this.liters=liters;this.cost=cost;this.full=full;}
    }
    static class Db extends SQLiteOpenHelper {
        Db(Context c){super(c,"fuel_log.db",null,1);}
        @Override public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE fuel (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, odo REAL NOT NULL, liters REAL NOT NULL, cost REAL NOT NULL, full INTEGER NOT NULL)");}
        @Override public void onUpgrade(SQLiteDatabase d,int old,int next){}
        void add(Fuel f){android.content.ContentValues v=new android.content.ContentValues();v.put("day",f.date);v.put("odo",f.odo);v.put("liters",f.liters);v.put("cost",f.cost);v.put("full",f.full?1:0);getWritableDatabase().insertOrThrow("fuel",null,v);}
        void remove(long id){getWritableDatabase().delete("fuel","id=?",new String[]{String.valueOf(id)});}
        ArrayList<Fuel> all(){ArrayList<Fuel> a=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id, day, odo, liters, cost, full FROM fuel ORDER BY odo ASC, id ASC",null)){while(c.moveToNext())a.add(new Fuel(c.getLong(0),c.getString(1),c.getDouble(2),c.getDouble(3),c.getDouble(4),c.getInt(5)==1));}return a;}
    }

    @Override public void onCreate(Bundle b){super.onCreate(b);db=new Db(this);chosenDate=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());build();refresh();}
    private int dp(float x){return Math.round(getResources().getDisplayMetrics().density*x);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String s,int sp,boolean bold,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private void addSpace(LinearLayout p,int h){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private TextView label(String s){TextView t=text(s,13,true,0xff3b4756);t.setPadding(0,dp(8),0,dp(4));return t;}
    private EditText input(String hint,boolean decimal){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(16);e.setHint(hint);e.setInputType(InputType.TYPE_CLASS_NUMBER|(decimal?InputType.TYPE_NUMBER_FLAG_DECIMAL:0));e.setPadding(dp(12),0,dp(12),0);e.setBackground(bg(0xffeff3f8,10));e.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(48)));return e;}
    private LinearLayout card(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(dp(18),dp(16),dp(18),dp(16));l.setBackground(bg(Color.WHITE,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(14));root.addView(l,p);return l;}
    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xfff3f6fb);setContentView(scroll);
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(14),dp(14),dp(14),dp(28));scroll.addView(root);
        root.addView(text("⛽ 내 차 연비 기록",23,true,0xff192a42));addSpace(root,3);
        root.addView(text("주유 입력부터 연비 확인까지 한 화면에서",12,false,0xff617085));addSpace(root,12);
        LinearLayout form=card();form.addView(text("오늘의 주유 입력",18,true,0xff192a42));addSpace(form,4);
        form.addView(label("주유 날짜"));dateButton=new Button(this);dateButton.setAllCaps(false);dateButton.setText(chosenDate+"   📅");form.addView(dateButton);dateButton.setOnClickListener(v->pickDate());
        form.addView(label("현재 계기판 누적 주행거리 (km)"));odoInput=input("예: 45,320",true);form.addView(odoInput);
        form.addView(label("주유량 (L)"));literInput=input("예: 35.5",true);form.addView(literInput);
        form.addView(label("총 주유금액 (킵, ₭)"));priceInput=input("예: 800000",false);form.addView(priceInput);
        fullInput=new CheckBox(this);fullInput.setText("가득 주유했어요 (만땅)");fullInput.setTextSize(14);fullInput.setChecked(true);form.addView(fullInput);
        Button save=new Button(this);save.setText("저장하고 연비 확인");save.setAllCaps(false);form.addView(save);save.setOnClickListener(v->save());
        tips=text("정확한 연비는 첫 번째 가득 주유를 기준점으로 잡고, 다음 가득 주유 때부터 계산합니다. 중간에 일부만 주유해도 자동으로 합산됩니다.",12,false,0xff677486);form.addView(tips);
        // 결과는 저장 버튼 바로 아래에서 같은 화면에 즉시 갱신됩니다.
        LinearLayout summary=card();summary.setBackground(bg(0xff193b68,18));
        headline=text("최근 2개월 평균 · 가득 주유 구간",13,false,0xffc9e1ff);summary.addView(headline);addSpace(summary,12);
        kmPerLiter=text("— km/L",31,true,Color.WHITE);summary.addView(kmPerLiter);
        costPerKm=text("— ₭/km",20,true,0xffb8e3ff);summary.addView(costPerKm);addSpace(summary,8);
        details=text("주유 내역을 등록해 주세요.",12,false,0xffe0efff);summary.addView(details);
        LinearLayout history=card();
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);history.addView(top);
        TextView title=text("주유 내역",18,true,0xff192a42);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button export=new Button(this);export.setAllCaps(false);export.setText("CSV 내보내기");export.setTextSize(12);top.addView(export);export.setOnClickListener(v->export());
        addSpace(history,10);entries=new LinearLayout(this);entries.setOrientation(1);history.addView(entries);
        root.addView(text("금액 단위: 라오스 킵(₭, LAK) · 기기 내부 저장 · 기록 길게 눌러 삭제",12,false,0xff718096));
    }
    private void pickDate(){Calendar c=Calendar.getInstance();try{c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(chosenDate));}catch(Exception ignored){}
        new DatePickerDialog(this,(view,y,m,d)->{chosenDate=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);dateButton.setText(chosenDate+"   📅");},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();}
    private double parse(EditText e){return Double.parseDouble(e.getText().toString().trim().replace(",",""));}
    private void save(){
        try{
            double odo=parse(odoInput),liters=parse(literInput),cost=parse(priceInput);
            if(odo<0||liters<=0||cost<=0||Double.isNaN(odo)||Double.isNaN(liters)||Double.isNaN(cost)||Double.isInfinite(odo)||Double.isInfinite(liters)||Double.isInfinite(cost))throw new Exception();
            ArrayList<Fuel> a=db.all();
            if(!a.isEmpty() && odo<=a.get(a.size()-1).odo){Toast.makeText(this,"주행거리는 이전 기록보다 커야 합니다.",Toast.LENGTH_LONG).show();return;}
            if(chosenDate.compareTo(new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()))>0){Toast.makeText(this,"미래 날짜는 입력할 수 없습니다.",Toast.LENGTH_LONG).show();return;}
            if(!a.isEmpty() && chosenDate.compareTo(a.get(a.size()-1).date)<0){Toast.makeText(this,"날짜는 이전 주유 기록 이후여야 합니다.",Toast.LENGTH_LONG).show();return;}
            db.add(new Fuel(0,chosenDate,odo,liters,cost,fullInput.isChecked()));
            odoInput.setText("");literInput.setText("");priceInput.setText("");fullInput.setChecked(true);refresh();Toast.makeText(this,"저장했습니다.",Toast.LENGTH_SHORT).show();
        }catch(Exception e){Toast.makeText(this,"주행거리·주유량·금액을 올바르게 입력해 주세요.",Toast.LENGTH_LONG).show();}
    }
    private void refresh(){
        ArrayList<Fuel> a=db.all();entries.removeAllViews();
        if(a.isEmpty()){entries.addView(text("아직 주유 기록이 없습니다.",14,false,0xff718096));}
        for(int i=a.size()-1;i>=0;i--){Fuel f=a.get(i);LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(dp(4),dp(11),dp(4),dp(11));
            l.addView(text(f.date+"  ·  "+one.format(f.odo)+" km",14,true,0xff2c3e56));
            l.addView(text(one.format(f.liters)+" L  /  "+money.format(f.cost)+" ₭  /  "+(f.full?"가득 주유":"부분 주유"),13,false,0xff5d6d80));
            entries.addView(l);View line=new View(this);line.setBackgroundColor(0xffe5ebf2);entries.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
            l.setOnLongClickListener(v->{new AlertDialog.Builder(this).setTitle("기록 삭제").setMessage(f.date+" 주유 기록을 삭제할까요?").setNegativeButton("취소",null).setPositiveButton("삭제",(dialog,w)->{db.remove(f.id);refresh();}).show();return true;});
        }
        Calendar limit=Calendar.getInstance();limit.add(Calendar.MONTH,-2);String cut=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(limit.getTime());
        Fuel start=null;double totalKm=0,totalL=0,totalCost=0;int intervals=0;
        double pendingL=0,pendingCost=0;
        for(Fuel f:a){
            if(start==null){if(f.full)start=f;continue;}
            pendingL+=f.liters;pendingCost+=f.cost;
            if(f.full){
                double km=f.odo-start.odo;
                if(km>0&&f.date.compareTo(cut)>=0){totalKm+=km;totalL+=pendingL;totalCost+=pendingCost;intervals++;}
                start=f;pendingL=0;pendingCost=0;
            }
        }
        if(totalL>0 && totalKm>0){kmPerLiter.setText(one.format(totalKm/totalL)+" km/L");costPerKm.setText(money.format(totalCost/totalKm)+" ₭/km");details.setText("완료된 주유 구간 "+intervals+"개 · "+one.format(totalKm)+" km 주행 · "+one.format(totalL)+" L 사용 · "+money.format(totalCost)+" ₭");}
        else{kmPerLiter.setText("— km/L");costPerKm.setText("— ₭/km");details.setText("최근 2개월 내 완료된 가득 주유 구간이 필요합니다.\n가득 주유 기록이 최소 2번 있어야 계산됩니다.");}
    }
    private void export(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/csv");i.putExtra(Intent.EXTRA_TITLE,"fuel_log_"+new SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date())+".csv");startActivityForResult(i,EXPORT_CSV);}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==EXPORT_CSV&&result==RESULT_OK&&data!=null){Uri uri=data.getData();if(uri==null)return;
        try(OutputStream os=getContentResolver().openOutputStream(uri);OutputStreamWriter w=new OutputStreamWriter(os,"UTF-8")){w.write("\uFEFF날짜,누적주행거리_km,주유량_L,금액_LAK,가득주유\n");for(Fuel f:db.all())w.write(String.format(Locale.US,"%s,%.2f,%.3f,%.0f,%s\n",f.date,f.odo,f.liters,f.cost,f.full?"예":"아니오"));w.flush();Toast.makeText(this,"CSV 저장 완료",Toast.LENGTH_LONG).show();}
        catch(Exception e){Toast.makeText(this,"내보내기에 실패했습니다.",Toast.LENGTH_LONG).show();}
    }}
}
