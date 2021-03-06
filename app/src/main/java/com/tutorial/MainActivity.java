package com.tutorial;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;

import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    /*
    Guarda o context desta activity
     */
    private static Context context;
    /*
    Vai mostrar uma especie de loading. Para não bloquear o ecrã ao utilizador.
     */
    ProgressDialog dialog;
    /*
    Pode ser um numero qualquer, escolhei o 1.
    */
    private static final int AUTH_CODE_REQUEST_CODE = 1;
    /*
    Key da folha do Google Drive.
    Ex: https://docs.google.com/spreadsheets/d/1QxLxCvhYGuH3vZ-CqRpJzL5ihhyLri8kGvSjwD3VU0c/
    */
    private static final String KEY_FOLHA = "1QxLxCvhYGuH3vZ-CqRpJzL5ihhyLri8kGvSjwD3VU0c";
    /*
    O token e uma string que é devolvida quando a autenticação é bem sucedida.
    */
    public static String token;
    /*
    Após a autenticação o service recolhe os dados do drive.
    É através dele que conseguimos ler e escrever nas folhas
    */
    public static SpreadsheetService service;
    /*
    Aqui vamos guardar a referencia para as folhas onde vamos trabalhar. Utilizado para folhas privadas
    */
    public static SpreadsheetEntry folhas = null;
    /*
    Aqui guardamos a referencia para uma folha de calculo. Utilizado para folhas publicas
    */
    public static WorksheetEntry folha = null;
    /*
    Nome da folha com que estamos a trabalhar. Este nome está nas tabs das folhas de calculo.
    */
    private static String nomeDaFolha = "Folha";
    /*
    Estes dois objetos servem para fazer a autenticação no drive.
    Um faz a autenticação o outro guarda os dados relativos à conta Google.
    */
    private static GoogleApiClient mGoogleApiClient;
    protected GoogleSignInAccount conta = null;
    /*
    Objeto que referncia o botão de login
    */
    private SignInButton btnSignIn;
    /*
    Guarda o nome da conta Google
     */
    private static String nomeDaConta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSignIn = (SignInButton) findViewById(R.id.btn_sign_in);
        btnSignIn.setOnClickListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        context = MainActivity.this;
    }

    // Devolve o nome da pessoa que esta associado a conta google
    public static String getNomeDaConta() {
        return nomeDaConta;
    }

    // Devolve o service com os dados das folhas.
    public static SpreadsheetService getService() {
        return service;
    }

    // Devolve as folhas contidas no drive
    public static SpreadsheetEntry getFolhas() {
        return folhas;
    }

    // Devolve a folha que estamos a trabalhar.
    public static WorksheetEntry getFolha() {
        return folha;
    }

    // onClick associado ao botao de login
    @Override
    public void onClick(View v) {
        signInWithGplus();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AUTH_CODE_REQUEST_CODE) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result != null && result.isSuccess()) {
                conta = result.getSignInAccount();
                nomeDaConta = conta.getDisplayName();
                getGoogleOAuthTokenAndLogin();
            } else {
                signInWithGplus();
            }
        }
    }


    // Mensagem de erro caso a autenticação falhe
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Toast.makeText(this, "Ocorreu um erro durante a ligação.", Toast.LENGTH_LONG).show();
    }

    // Faz login com a conta google
    private void signInWithGplus() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, AUTH_CODE_REQUEST_CODE);
    }

    // Faz logout da conta google e volta ao ecra de autenticação
    protected static void signOutFromGplus() {
        token = null;
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                    }
                });
    }

    private void getGoogleOAuthTokenAndLogin() {
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

            /*
             É executado antes do doInBackground
             Vai mostrar o loading até que a autenticação acabe.
             */
            @Override
            protected void onPreExecute() {
                dialog = new ProgressDialog(context);
                dialog.setMessage("A autenticar...");
                dialog.setIndeterminate(true);
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
            }

            // Tenta autenticar no Drive. Se conseguir devolve um token != null
            @Override
            protected String doInBackground(Void... params) {
                try {
                    boolean autenticado = false;

                    // Usar este para folhas privadas
                    //URL SPREADSHEET_FEED_URL = new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full");

                    // Usar este para folhas publicas
                    URL SPREADSHEET_FEED_URL = new URL("https://spreadsheets.google.com/feeds/worksheets/"+ KEY_FOLHA +"/private/full");

                    String scope = "oauth2:https://spreadsheets.google.com/feeds";
                    token = GoogleAuthUtil.getToken(MainActivity.this, conta.getEmail(), scope);
                    service = new SpreadsheetService("MySpreadsheetIntegration-v3");
                    service.setHeader("Authorization", "Bearer " + token);

                ///////////////////////////////////////////////////////////////////////////////////
                /*
                    Esta Parte é a que devemos utilizar para as folhas publicas
                */

                    WorksheetFeed feed = service.getFeed(SPREADSHEET_FEED_URL, WorksheetFeed.class);
                    List<WorksheetEntry> worksheets = feed.getEntries();
                    for (WorksheetEntry worksheet : worksheets)
                            if(worksheet.getTitle().getPlainText().equals(nomeDaFolha)) {
                                folha = worksheet;
                                autenticado = true;
                                break;
                            }

                    if (!autenticado || folha == null) {
                        token = null;
                    }


                ///////////////////////////////////////////////////////////////////////////////////
                /*
                    Esta parte é a que devemos utilizar para as folhas privadas
                */
/*
                    SpreadsheetFeed feed = service.getFeed(SPREADSHEET_FEED_URL, SpreadsheetFeed.class);
                    List<SpreadsheetEntry> spreadsheets = feed.getEntries();
                    for (SpreadsheetEntry spreadsheet : spreadsheets) {
                        if (spreadsheet.getKey().equals("1vgSF2baarOk17Ox-pMmjDmsWtHFqdEUxYCzfY9KMcPg")) {
                            folhas = spreadsheet;
                            autenticado = true;
                            break;
                        }
                    }

                    if (!autenticado || folhas == null) {
                        token = null;
                    }
*/

                } catch (UserRecoverableAuthException transientEx) {
                    startActivityForResult(transientEx.getIntent(), AUTH_CODE_REQUEST_CODE);
                } catch (Exception authEx) {
                    authEx.printStackTrace();
                }
                return token;
            }

            // É executado depois do doInBackground
            @Override
            protected void onPostExecute(String token) {
                if(token != null ) {
                    startActivity(new Intent(context, Main2Activity.class));
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "Ocorreu uma erro durante a autenticaçao.", Toast.LENGTH_LONG);
                }
                dialog.dismiss();
            }
        };
        task.execute();
    }
}