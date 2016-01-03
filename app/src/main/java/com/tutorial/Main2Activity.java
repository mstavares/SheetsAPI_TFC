package com.tutorial;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class Main2Activity extends MainActivity {

    private static final int LINHA_DA_CELULA_A1 = 1;
    private static final int COLUNA_DA_CELULA_A1 = 1;
    private static Context context;
    private static ProgressDialog dialog;
    private static EditText edtxtDados;
    private static String aEscreverNaCelula;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        context = Main2Activity.this;
        edtxtDados = (EditText) findViewById(R.id.edtxtDados);
    }

    public void btnLer_onClick (View view) {
        interagirComAFolha(true);
    }

    public void btnEscrever_onClick (View view) {
        aEscreverNaCelula = edtxtDados.getText().toString();
        interagirComAFolha(false);
    }

    public void btnLogout_onClick (View view) {
        signOutFromGplus();
        startActivity(new Intent(context, MainActivity.class));
        finish();
    }

    private static String restringeLinhasEColunas(int linhaMin, int linhaMax, int ColunaMin, int ColunaMax) {
        return "?min-row=" + linhaMin + "&max-row=" + linhaMax + "&min-col=" + ColunaMin + "&max-col=" + ColunaMax;
    }

    private static void interagirComAFolha(final boolean ler) {
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            WorksheetEntry folhaEscolhida;
            List<CellEntry> celulasLidas;
            URL feedURL;

            @Override
            protected void onPreExecute() {
                dialog = new ProgressDialog(context);
                dialog.setMessage("A carregar os dados...");
                dialog.setIndeterminate(true);
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
            }

            @Override
            protected String doInBackground(Void... params) {
                CellFeed feedCell;
                try {

                    ///////////////////////////////////////////////////////////////////////////////////
                /*
                    Esta parte é a que devemos utilizar para as folhas privadas
                */
/*
                    // o contains ten de ter o nome da folha privada que vamos ler
                    for (WorksheetEntry worksheet : MainActivity.getFolhas().getWorksheets())
                        if (worksheet.getTitle().getPlainText().contains("Folha")) {
                            folhaEscolhida = worksheet;
                            break;
                        }
                    if (folhaEscolhida != null) {
                        feedURL = new URI(folhaEscolhida.getCellFeedUrl().toString() + restringeLinhasEColunas(LINHA_DA_CELULA_A1, LINHA_DA_CELULA_A1,
                                COLUNA_DA_CELULA_A1, COLUNA_DA_CELULA_A1)).toURL();
                        feedCell = MainActivity.service.getFeed(feedURL, CellFeed.class);
                        celulasLidas = feedCell.getEntries();
                    }
*/
                 ///////////////////////////////////////////////////////////////////////////////////
                /*
                    Esta Parte é a que devemos utilizar para as folhas publicas
                */

                    folhaEscolhida = MainActivity.getFolha();
                    feedURL = new URI(folhaEscolhida.getCellFeedUrl().toString() + restringeLinhasEColunas(LINHA_DA_CELULA_A1, LINHA_DA_CELULA_A1,
                        COLUNA_DA_CELULA_A1, COLUNA_DA_CELULA_A1)).toURL();
                    feedCell = MainActivity.service.getFeed(feedURL, CellFeed.class);
                    celulasLidas = feedCell.getEntries();


                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ServiceException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                return "sucesso";
            }

            @Override
            protected void onPostExecute(String result) {
                if (folhaEscolhida != null) {
                    if (ler)
                        lerDaCelula(celulasLidas);
                    else {
                        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
                            @Override
                            protected String doInBackground(Void... params) {
                                escreverNaCelula(feedURL, LINHA_DA_CELULA_A1, COLUNA_DA_CELULA_A1);
                                return "sucesso";
                            }
                        };
                        task.execute();
                    }
                }
                dialog.dismiss();
            }
        };
        task.execute();
    }

    private static  void lerDaCelula (List<CellEntry> celulasLidas) {
        for(CellEntry celula : celulasLidas)
            edtxtDados.setText(celula.getCell().getValue());
    }

    private static void escreverNaCelula(URL feedURL, int linha, int coluna) {
        try {
            MainActivity.getService().insert(feedURL, new CellEntry(linha, coluna, aEscreverNaCelula));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }
}
