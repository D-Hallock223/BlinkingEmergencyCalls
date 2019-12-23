package com.example.blinkingemergencycalls;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SplashSettings extends AppCompatActivity {

    private static final String BLINKCOUNT = "BlinkCount:";
    private static final int WARRING_MESSAGE = 1;
    private static final int YESNO_MESSAGE = 2;
    private static String WARRING_TITLE = "Warring";
    public static GlobalVariable primaryValue = new GlobalVariable();
    public List<ContactDTO> contractList = new ArrayList<ContactDTO>();
    ListView listview;
    String[] ListViewItems;
    SparseBooleanArray sparseBooleanArray;
    int blinkCount = 0;
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_settings);

        setTitle(R.string.title);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mPrefs.edit();

        if(!mPrefs.getBoolean("bFirst",false)){
            formatPreferenceValue();
        }
        else {
            primaryValue = getGlobalVariable();
            startMainActivity();
        }
    }
    private void confirmSettings()
    {
        if (!hasPhoneContactsPermission(Manifest.permission.READ_CONTACTS)) {
            requestPermission(Manifest.permission.READ_CONTACTS);
        } else {
            ProgressDialog progress = new ProgressDialog(this);
            progress.setTitle("Loading Contact");
            progress.setMessage("Wait while loading...");
            progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
            progress.show();

            getAllContacts();
            if (!contractList.isEmpty())
                initContractList();

            progress.dismiss();
        }
        SeekBar seekBar = (SeekBar) findViewById(R.id.contractCount);
        final TextView tv = (TextView) findViewById(R.id.tvBlinkCount);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv.setText(BLINKCOUNT + String.valueOf(progress));
                blinkCount = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        Button btnConfirm = (Button) findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (blinkCount == 0)
                {
                    String title = WARRING_TITLE;
                    String message = "Please Select Blink Count.";
                    popAlertDialog(title, message,WARRING_MESSAGE);
                }
                else {
                    sparseBooleanArray = listview.getCheckedItemPositions();
                    List<String> phoneList = new ArrayList<>();
                    mEditor.putInt("contractCount", sparseBooleanArray.size());
                    mEditor.commit();
                    for (int i = 0; i < sparseBooleanArray.size(); i++) {
                        if(sparseBooleanArray.valueAt(i)) {
                            String key = "Phone" + (i + 1);
                            String value =  contractList.get(sparseBooleanArray.keyAt(i)).getPhoneList().get(0).getDataValue();
                            mEditor.putString(key, contractList.get(sparseBooleanArray.keyAt(i)).getPhoneList().get(0).getDataValue());
                            mEditor.commit();
                            phoneList.add(contractList.get(sparseBooleanArray.keyAt(i)).getPhoneList().get(0).getDataValue());
                        }
                    }
                    mEditor.putInt("blinkCount",blinkCount);
                    mEditor.putBoolean("bFirst",true);
                    mEditor.commit();
                    primaryValue.setbFirst(true);
                    primaryValue.setBlinkCount(blinkCount);
                    primaryValue.setPhoneNumbers(phoneList);
                    startMainActivity();
                }
            }
        });
    }
    private void popAlertDialog(String title,String message,int type){
        AlertDialog.Builder builder = new AlertDialog.Builder(SplashSettings.this);
        builder.setTitle(title);
        builder.setMessage(message);
        switch(type){
            case WARRING_MESSAGE:
                builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                break;
            case YESNO_MESSAGE:
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.setNegativeButton("No",null);
                break;
        }
        builder.create();
        builder.show();
    }
    private void formatPreferenceValue() {
        mEditor.putInt("blinkCount",0);
        mEditor.commit();
        mEditor.putString("Phone1","");
        mEditor.commit();
        mEditor.putString("Phone2","");
        mEditor.commit();
        mEditor.putString("Phone3","");
        mEditor.commit();
        mEditor.putString("Phone4","");
        mEditor.commit();
        mEditor.putString("Phone5","");
        mEditor.commit();
        confirmSettings();
    }
    private GlobalVariable getGlobalVariable()
    {
        GlobalVariable temp = new GlobalVariable();
        temp.setBlinkCount(mPrefs.getInt("blinkCount",0));
        List<String> phoneList = new ArrayList<>();
        for (int i = 0; i < 5; i++){
            String key = "Phone" + (i + 1);
            String tempStr = mPrefs.getString(key,"");
            if ( tempStr != "")
                phoneList.add(tempStr);
            else break;
        }
        temp.setPhoneNumbers(phoneList);
        return temp;
    }
    private void startMainActivity(){
        //primaryValue = getGlobalVariable();
        Intent mMainActivity = new Intent(SplashSettings.this, MainActivity.class);
        startActivity(mMainActivity);
        finish();
    }
    private void initContractList(){

        ListViewItems = new String[contractList.size()];
        for(int i = 0; i < contractList.size(); i++) {
            if(contractList.get(i).getGivenName().isEmpty() && contractList.get(i).getFamilyName().isEmpty()){
                if(!contractList.get(i).getPhoneList().isEmpty()){
                    ListViewItems[i] += contractList.get(i).getPhoneList().get(i).getDataValue();
                }
                else ListViewItems[i] += "No Phone Number";
            }
            else {
                String temp1 = contractList.get(i).getGivenName()== null? "" : contractList.get(i).getGivenName();
                String temp2 = (contractList.get(i).getFamilyName() == null ? "" : contractList.get(i).getFamilyName());
                ListViewItems[i] = temp1 + temp2;
                //ListViewItems[i] += (contractList.get(i).getGivenName()== null? "" : contractList.get(i).getGivenName()) + (contractList.get(i).getFamilyName() == null ? "" : contractList.get(i).getFamilyName());
            }
        }
        listview = (ListView) findViewById(R.id.contractListView);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>
                (SplashSettings.this,
                        android.R.layout.simple_list_item_multiple_choice,
                        android.R.id.text1, ListViewItems);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SparseBooleanArray temp = listview.getCheckedItemPositions();
                int checkedCount = 0;
                for (int i = 0; i < temp.size(); i++)
                    if(temp.valueAt(i) == true)
                        checkedCount ++;
                if(checkedCount > 5)
                    listview.setItemChecked(position,false);
            }
        });

    }
    // Check whether user has phone contacts manipulation permission or not.
    private boolean hasPhoneContactsPermission(String permission)
    {
        boolean ret = false;

        // If android sdk version is bigger than 23 the need to check run time permission.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            // return phone read contacts permission grant status.
            int hasPermission = ContextCompat.checkSelfPermission(getApplicationContext(), permission);
            // If permission is granted then return true.
            if (hasPermission == PackageManager.PERMISSION_GRANTED) {
                ret = true;
            }
        }else
        {
            ret = true;
        }
        return ret;
    }
    private void getSIMContact(List<ContactDTO> ret){
        try
        {
            String ClsSimPhonename = null;
            String ClsSimphoneNo = null;

            Uri simUri = Uri.parse("content://icc/adn");
            Cursor cursorSim = this.getContentResolver().query(simUri,null,null,null,null);

            Log.i("PhoneContact", "total: "+cursorSim.getCount());

            while (cursorSim.moveToNext())
            {
                ContactDTO dto = new ContactDTO();
                ClsSimPhonename =cursorSim.getString(cursorSim.getColumnIndex("name"));
                ClsSimphoneNo = cursorSim.getString(cursorSim.getColumnIndex("number"));
                ClsSimphoneNo.replaceAll("\\D","");
                ClsSimphoneNo.replaceAll("&", "");
                ClsSimPhonename=ClsSimPhonename.replace("|","");
                List<DataDTO> phoneList = new ArrayList<>();
                DataDTO phone = new DataDTO();
                phone.setDataValue(ClsSimphoneNo);
                phone.setDataType(1);
                phoneList.add(phone);

                dto.setPhoneList(phoneList);
                dto.setDisplayName(ClsSimPhonename);

                ret.add(dto);
                Log.i("PhoneContact", "name: "+ClsSimPhonename+" phone: "+ClsSimphoneNo);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    /* Return all contacts and show each contact data in android monitor console as debug info. */
    private List<ContactDTO> getAllContacts()
    {

        List<ContactDTO> ret = new ArrayList<ContactDTO>();
        //getSIMContact(ret);

        // Get all raw contacts id list.
        List<Integer> rawContactsIdList = getRawContactsIdList();

        int contactListSize = rawContactsIdList.size();

        ContentResolver contentResolver = getContentResolver();

        // Loop in the raw contacts list.
        for(int i=0;i<contactListSize;i++)
        {
            // Get the raw contact id.
            Integer rawContactId = rawContactsIdList.get(i);

            //Log.e("SplashSettings", "raw contact id : " + rawContactId.intValue());

            // Data content uri (access data table. )
            Uri dataContentUri = ContactsContract.Data.CONTENT_URI;

            // Build query columns name array.
            List<String> queryColumnList = new ArrayList<String>();

            // ContactsContract.Data.CONTACT_ID = "contact_id";
            queryColumnList.add(ContactsContract.Data.CONTACT_ID);

            // ContactsContract.Data.MIMETYPE = "mimetype";
            queryColumnList.add(ContactsContract.Data.MIMETYPE);

            queryColumnList.add(ContactsContract.Data.DATA1);
            queryColumnList.add(ContactsContract.Data.DATA2);
            queryColumnList.add(ContactsContract.Data.DATA3);
            queryColumnList.add(ContactsContract.Data.DATA4);
            queryColumnList.add(ContactsContract.Data.DATA5);
            queryColumnList.add(ContactsContract.Data.DATA6);
            queryColumnList.add(ContactsContract.Data.DATA7);
            queryColumnList.add(ContactsContract.Data.DATA8);
            queryColumnList.add(ContactsContract.Data.DATA9);
            queryColumnList.add(ContactsContract.Data.DATA10);
            queryColumnList.add(ContactsContract.Data.DATA11);
            queryColumnList.add(ContactsContract.Data.DATA12);
            queryColumnList.add(ContactsContract.Data.DATA13);
            queryColumnList.add(ContactsContract.Data.DATA14);
            queryColumnList.add(ContactsContract.Data.DATA15);

            // Translate column name list to array.
            String queryColumnArr[] = queryColumnList.toArray(new String[queryColumnList.size()]);

            // Build query condition string. Query rows by contact id.
            StringBuffer whereClauseBuf = new StringBuffer();
            whereClauseBuf.append(ContactsContract.Data.RAW_CONTACT_ID);
            whereClauseBuf.append("=");
            whereClauseBuf.append(rawContactId);

            // Query data table and return related contact data.
            Cursor cursor = contentResolver.query(dataContentUri, queryColumnArr, whereClauseBuf.toString(), null, null);

            /* If this cursor return database table row data.
               If do not check cursor.getCount() then it will throw error
               android.database.CursorIndexOutOfBoundsException: Index 0 requested, with a size of 0.
               */
            ContactDTO temp = new ContactDTO();
            List<DataDTO> tempDto = new ArrayList<DataDTO>();
            List<DataDTO> tempPhone = new ArrayList<DataDTO>();
            if(cursor!=null && cursor.getCount() > 0)
            {
                StringBuffer lineBuf = new StringBuffer();
                cursor.moveToFirst();

                lineBuf.append("Raw Contact Id : ");
                lineBuf.append(rawContactId);

                long contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                lineBuf.append(" , Contact Id : ");
                lineBuf.append(contactId);


                do{
                    // First get mimetype column value.
                    String mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
                    lineBuf.append(" \r\n , MimeType : ");
                    lineBuf.append(mimeType);

                    List<String> dataValueList = getColumnValueByMimetype(cursor, mimeType,temp,tempDto,tempPhone);
                    int dataValueListSize = dataValueList.size();
                    for(int j=0;j < dataValueListSize;j++)
                    {
                        String dataValue = dataValueList.get(j);
                        lineBuf.append(" , ");
                        lineBuf.append(dataValue);
                    }

                }while(cursor.moveToNext());
                temp.setEmailList(tempDto);
                temp.setPhoneList(tempPhone);
                contractList.add(temp);

                //Log.e("asdf", lineBuf.toString());
            }

            //Log.e("asdf", "=========================================================================");
        }
        return ret;
    }
    private void requestPermission(String permission)
    {
        String requestPermissionArray[] = {permission};
        ActivityCompat.requestPermissions(this, requestPermissionArray, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode,
                permissions,
                grantResults);

        if (requestCode == 1) {

            // Checking whether user granted the permission or not.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Showing the toast message

                ProgressDialog progress = new ProgressDialog(this);
                progress.setTitle("Loading Contact");
                progress.setMessage("Wait while loading...");
                progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
                progress.show();

                getAllContacts();
                if (!contractList.isEmpty())
                    initContractList();

                progress.dismiss();
            }
            else {
                Toast.makeText(SplashSettings.this,
                        "Contact Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }


    private List<Integer> getRawContactsIdList()
    {
        List<Integer> ret = new ArrayList<Integer>();

        ContentResolver contentResolver = getContentResolver();

        // Row contacts content uri( access raw_contacts table. ).
        Uri rawContactUri = ContactsContract.RawContacts.CONTENT_URI;
        // Return _id column in contacts raw_contacts table.
        String queryColumnArr[] = {ContactsContract.RawContacts._ID};
        // Query raw_contacts table and return raw_contacts table _id.
        Cursor cursor = contentResolver.query(rawContactUri,queryColumnArr, null, null, null);
        if(cursor!=null)
        {
            cursor.moveToFirst();
            do{
                int idColumnIndex = cursor.getColumnIndex(ContactsContract.RawContacts._ID);
                int rawContactsId = cursor.getInt(idColumnIndex);
                ret.add(new Integer(rawContactsId));
            }while(cursor.moveToNext());
        }

        cursor.close();

        return ret;
    }
    /*
     *  Return data column value by mimetype column value.
     *  Because for each mimetype there has not only one related value,
     *  such as Organization.CONTENT_ITEM_TYPE need return company, department, title, job description etc.
     *  So the return is a list string, each string for one column value.
     * */
    private List<String> getColumnValueByMimetype(Cursor cursor, String mimeType,ContactDTO temp, List<DataDTO> tempDto, List<DataDTO> tempPhone)
    {
        List<String> ret = new ArrayList<String>();

        switch (mimeType)
        {
            // Get email data.
            case ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE :
                // Email.ADDRESS == data1
                String emailAddress = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                // Email.TYPE == data2
                int emailType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
                String emailTypeStr = getEmailTypeString(emailType);
                DataDTO tempEmail = new DataDTO();
                tempEmail.setDataType(emailType);
                tempEmail.setDataValue(emailAddress);
                tempDto.add(tempEmail);

                ret.add("Email Address : " + emailAddress);
                ret.add("Email Int Type : " + emailType);
                ret.add("Email String Type : " + emailTypeStr);
                break;

            // Get im data.
            case ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE:
                // Im.PROTOCOL == data5
                String imProtocol = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.PROTOCOL));
                // Im.DATA == data1
                String imId = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA));

                ret.add("IM Protocol : " + imProtocol);
                ret.add("IM ID : " + imId);
                break;

            // Get nickname
            case ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE:
                // Nickname.NAME == data1
                String nickName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME));
                ret.add("Nick name : " + nickName);
                temp.setNickName(nickName);
                break;

            // Get organization data.
            case ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE:
                // Organization.COMPANY == data1
                String company = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY));
                // Organization.DEPARTMENT == data5
                String department = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DEPARTMENT));
                // Organization.TITLE == data4
                String title = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE));
                // Organization.JOB_DESCRIPTION == data6
                String jobDescription = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION));
                // Organization.OFFICE_LOCATION == data9
                String officeLocation = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION));

                temp.setCompany(company);
                temp.setDepartment(department);
                temp.setTitle(title);
                temp.setJobDescription(jobDescription);
                temp.setOfficeLocation(officeLocation);

                ret.add("Company : " + company);
                ret.add("department : " + department);
                ret.add("Title : " + title);
                ret.add("Job Description : " + jobDescription);
                ret.add("Office Location : " + officeLocation);
                break;

            // Get phone number.
            case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE:
                // Phone.NUMBER == data1
                String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                // Phone.TYPE == data2
                int phoneTypeInt = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                String phoneTypeStr = getPhoneTypeString(phoneTypeInt);

                DataDTO tempPhoneD = new DataDTO();
                tempPhoneD.setDataType(phoneTypeInt);
                tempPhoneD.setDataValue(phoneNumber);
                tempPhone.add(tempPhoneD);

                ret.add("Phone Number : " + phoneNumber);
                ret.add("Phone Type Integer : " + phoneTypeInt);
                ret.add("Phone Type String : " + phoneTypeStr);
                break;

            // Get sip address.
            case ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE:
                // SipAddress.SIP_ADDRESS == data1
                String address = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS));
                // SipAddress.TYPE == data2
                int addressTypeInt = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.TYPE));
                String addressTypeStr = getEmailTypeString(addressTypeInt);


                ret.add("Address : " + address);
                ret.add("Address Type Integer : " + addressTypeInt);
                ret.add("Address Type String : " + addressTypeStr);
                break;

            // Get display name.
            case ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE:
                // StructuredName.DISPLAY_NAME == data1
                String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
                // StructuredName.GIVEN_NAME == data2
                String givenName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                // StructuredName.FAMILY_NAME == data3
                String familyName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));

                temp.setDisplayName(displayName);
                temp.setGivenName(givenName);
                temp.setFamilyName(familyName);
                ret.add("Display Name : " + displayName);
                ret.add("Given Name : " + givenName);
                ret.add("Family Name : " + familyName);
                break;

            // Get postal address.
            case ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE:
                // StructuredPostal.COUNTRY == data10
                String country = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));
                // StructuredPostal.CITY == data7
                String city = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
                // StructuredPostal.REGION == data8
                String region = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION));
                // StructuredPostal.STREET == data4
                String street = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
                // StructuredPostal.POSTCODE == data9
                String postcode = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
                // StructuredPostal.TYPE == data2
                int postType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE));
                String postTypeStr = getEmailTypeString(postType);

                temp.setCountry(country);
                temp.setCity(city);
                temp.setRegion(region);
                temp.setStreet(street);
                temp.setRegion(region);
                temp.setPostCode(postcode);
                temp.setPostType(postType);

                ret.add("Country : " + country);
                ret.add("City : " + city);
                ret.add("Region : " + region);
                ret.add("Street : " + street);
                ret.add("Postcode : " + postcode);
                ret.add("Post Type Integer : " + postType);
                ret.add("Post Type String : " + postTypeStr);
                break;

            // Get identity.
            case ContactsContract.CommonDataKinds.Identity.CONTENT_ITEM_TYPE:
                // Identity.IDENTITY == data1
                String identity = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Identity.IDENTITY));
                // Identity.NAMESPACE == data2
                String namespace = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Identity.NAMESPACE));
                temp.setIdentity(identity);
                temp.setNamespace(namespace);

                ret.add("Identity : " + identity);
                ret.add("Identity Namespace : " + namespace);
                break;

            // Get photo.
            case ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE:
                // Photo.PHOTO == data15
                byte[] photo = cursor.getBlob(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO));
                // Photo.PHOTO_FILE_ID == data14
                String photoFileId = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO_FILE_ID));

                ret.add("Photo : " + photo);
                ret.add("Photo File Id: " + photoFileId);
                break;

            // Get group membership.
            case ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE:
                // GroupMembership.GROUP_ROW_ID == data1
                int groupId = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID));
                temp.setGroupId(groupId);
                ret.add("Group ID : " + groupId);
                break;

            // Get website.
            case ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE:
                // Website.URL == data1
                String websiteUrl = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Website.URL));
                // Website.TYPE == data2
                int websiteTypeInt = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Website.TYPE));
                String websiteTypeStr = getEmailTypeString(websiteTypeInt);

                ret.add("Website Url : " + websiteUrl);
                ret.add("Website Type Integer : " + websiteTypeInt);
                ret.add("Website Type String : " + websiteTypeStr);
                break;

            // Get note.
            case ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE:
                // Note.NOTE == data1
                String note = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE));
                ret.add("Note : " + note);
                break;

        }
        temp.setPhoneList(tempPhone);
        temp.setEmailList(tempDto);
        return ret;
    }
    /*
     *  Get email type related string format value.
     * */
    private String getEmailTypeString(int dataType)
    {
        String ret = "";

        if(ContactsContract.CommonDataKinds.Email.TYPE_HOME == dataType)
        {
            ret = "Home";
        }else if(ContactsContract.CommonDataKinds.Email.TYPE_WORK==dataType)
        {
            ret = "Work";
        }
        return ret;
    }
    /*
     *  Get phone type related string format value.
     * */
    private String getPhoneTypeString(int dataType)
    {
        String ret = "";

        if(ContactsContract.CommonDataKinds.Phone.TYPE_HOME == dataType)
        {
            ret = "Home";
        }else if(ContactsContract.CommonDataKinds.Phone.TYPE_WORK==dataType)
        {
            ret = "Work";
        }else if(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE==dataType)
        {
            ret = "Mobile";
        }
        return ret;
    }
}
