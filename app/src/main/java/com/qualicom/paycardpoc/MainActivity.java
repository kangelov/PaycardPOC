package com.qualicom.paycardpoc;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.qualicom.emvpaycard.EmvPayCardException;
import com.qualicom.emvpaycard.business.CardData;
import com.qualicom.emvpaycard.command.GetProcessingOptionsCommand;
import com.qualicom.emvpaycard.command.PayCardCommand;
import com.qualicom.emvpaycard.command.ReadRecordCommand;
import com.qualicom.emvpaycard.command.SelectCommand;
import com.qualicom.emvpaycard.data.ApplicationFileLocatorRange;
import com.qualicom.emvpaycard.data.FCIApplicationTemplate;
import com.qualicom.emvpaycard.data.GetProcessingOptionsResponse;
import com.qualicom.emvpaycard.data.ReadResponse;
import com.qualicom.emvpaycard.data.SelectResponse;
import com.qualicom.emvpaycard.utils.ByteString;
import com.qualicom.emvpaycard.utils.PayCardUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TECH_NFCA = "android.nfc.tech.NfcA";
    public static final String TECH_NFCB = "android.nfc.tech.NfcB";
    public static final String TECH_UNKNOWN = "???";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        handleIntent(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private synchronized void handleIntent(Intent intent) {
        if (intent != null && NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            Tag tag = (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Toast toast = null;

            try {
                if (PayCardUtils.isValidEmvPayCard(tag)) {
                    if (PayCardUtils.isTypeAPayCard(tag))
                        toast = Toast.makeText(this, "This is a valid NFC type A payment card.", Toast.LENGTH_SHORT);
                    if (PayCardUtils.isTypeBPayCard(tag))
                        toast = Toast.makeText(this, "This is a valid NFC type A payment card.", Toast.LENGTH_SHORT);

                    PayCardCommand payCardCommand = new PayCardCommand(tag);
                    payCardCommand.connect();
                    SelectCommand selectCommand = new SelectCommand(payCardCommand);
                    SelectResponse ddfResponse = selectCommand.selectDDF(SelectCommand.PPSE);
                    Log.i("DDF RESPONSE", ddfResponse.toString());

                    StringBuffer buffer = new StringBuffer();

                    for (FCIApplicationTemplate ddfAppTemplate : ddfResponse.getFciTemplate().getFciProprietaryTemplate().getIssuerDiscretionaryData().getApplicationTemplateData()) {
                        buffer.append("\n --- START APPLICATION --- \n");
                        String appId = ddfAppTemplate.getAdfName();
                        SelectResponse adfResponse = selectCommand.selectADF(ByteString.hexStringToByteArray(appId)); //Mastercard.
                        Log.i("ADF RESPONSE", adfResponse.toString());

                        //Read the GPO for the application selected using an empty PDOL.
                        GetProcessingOptionsCommand gpoController = new GetProcessingOptionsCommand(payCardCommand);
                        GetProcessingOptionsResponse gpoResponse = gpoController.getApplicationProfile("8300");
                        Log.i("GPO RESPONSE", gpoResponse.toString());

                        //Read every record range returned by GPO above.
                        ReadRecordCommand readRecordCommand = new ReadRecordCommand(payCardCommand);
                        List<ReadResponse> readResponses = new ArrayList<ReadResponse>();

                        if (gpoResponse.isSuccessfulResponse()) {
                            for (ApplicationFileLocatorRange range : gpoResponse.getApplicationFileLocator().getRecordRanges()) {
                                ReadResponse readResponse = readRecordCommand.readRecord(range);
                                readResponses.add(readResponse);
                                Log.i("RR RESPONSE", readResponse.toString());
                            }
                        } else {
                            buffer.append(" !!! GPO returns error. Attempting to guess application records to read.");
                            ReadResponse readResponse = readRecordCommand.readRecord(
                                    (byte) 01,
                                    (byte) 01,
                                    (byte) 00);
                            readResponses.add(readResponse);
                            Log.i("RR RESPONSE", readResponse.toString());

                        }

                        //Print everything.
                        for(ReadResponse readResponse : readResponses) {
                            if (readResponse.isSuccessfulResponse()) {
                                CardData cardData = new CardData(
                                        readResponse.getApplicationData(),
                                        ddfResponse.getFciTemplate().getFciProprietaryTemplate(),
                                        ddfAppTemplate,
                                        adfResponse.getFciTemplate().getFciProprietaryTemplate());
                                Log.i("DATA", cardData.toString());
                                buffer.append("\n >>> START RECORD: \n" + cardData.toString() + "\n >>> END RECORD \n");
                            } else {
                                buffer.append("\n >>> START RECORD: \n" + readResponse.getStatusWord() + " " + readResponse.getStatusMessage() + "\n >>> END RECORD \n");
                            }
                        }
                        buffer.append("\n --- END APPLICATION --- \n");
                    }


                    TextView textView = (TextView) findViewById(R.id.content_text);
                    textView.setText(buffer.toString());

                    payCardCommand.disconnect();

                } else
                    toast = Toast.makeText(this, "This is an invalid or unknown payment card.", Toast.LENGTH_SHORT);
                toast.show();
            } catch (EmvPayCardException e) {
                Log.e("ERROR",e.getMessage());
                TextView textView = (TextView) findViewById(R.id.content_text);
                textView.setText(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
