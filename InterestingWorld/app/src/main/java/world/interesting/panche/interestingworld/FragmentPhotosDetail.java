package world.interesting.panche.interestingworld;

/**
 * Created by Alex on 15/01/2015.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.Gravity;
import android.view.LayoutInflater;

import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.cuneytayyildiz.widget.PullRefreshLayout;
import com.devspark.appmsg.AppMsg;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.melnykov.fab.FloatingActionButton;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;

public class FragmentPhotosDetail extends Fragment implements FragmentImageViewer.onRateImage {
    View inflatedView;
    GridViewAdapterImages gridAdapter;
    private SweetAlertDialog pDialog;
    ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();
    private final List<String> urls = new ArrayList<String>();
    public List<String> select_image = new ArrayList<String>();
    FragmentManager fm;
    FloatingActionButton fab;
    MenuItem selected;
    PullRefreshLayout layout;
    GridView gv;
    String result,id;
    String[] datos= new String[5];
    Uri selectedImage;
    InputStream is;
    File image;
    View emptyView;
    AsyncHttpClient client=new AsyncHttpClient();
    ImageButton reload;


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        inflatedView = inflater.inflate(R.layout.photos_list_detail, container, false);
        TextView text = new TextView(this.getActivity());
        text.setText(this.getResources().getString(R.string.photos));
        text.setGravity(Gravity.CENTER);
        setHasOptionsMenu(true);
        fm= this.getActivity().getSupportFragmentManager();

        emptyView = (View) inflatedView.findViewById(R.id.empty_view);
        gridAdapter=new GridViewAdapterImages(this.getActivity());


        gv = (GridView) inflatedView.findViewById(R.id.grid_view);
        gv.setAdapter(gridAdapter);
        gv.setOnScrollListener(new SampleScrollListener(this.getActivity()));
        gv.setEmptyView(emptyView);
        reload = (ImageButton) inflatedView.findViewById(R.id.ic1);

        // listen refresh event
        reload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadData();
            }
        });

        fab= (FloatingActionButton) inflatedView.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(getActivity().getLocalClassName().equals("MainActivityUser")) {
                    selectImage(v);
                }else{
                    AppMsg.makeText(getActivity(), "Debes estar logueado para poder introducir un comentario", AppMsg.STYLE_ALERT).setLayoutGravity(Gravity.BOTTOM).show();
                }
            }
        });
        Location loc;
        if(this.getActivity().getLocalClassName().equals("MainActivity")) {
            loc = ((MainActivity) getActivity()).GetLocationSelected();
        }else{
            loc = ((MainActivityUser) getActivity()).GetLocationSelected();
        }
        id= loc.getId();


        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                select_image=gridAdapter.getInfoSelectedPhoto(position);
                ViewFull();
            }
        });

        layout = (PullRefreshLayout) inflatedView.findViewById(R.id.swipeRefreshLayout);

        // listen refresh event
        layout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                urls.clear();
                loadData();

            }
        });
        urls.clear();
        loadData();
        return inflatedView;
    }
    @Override
    public void onDestroy() {
        client.cancelAllRequests(true);
        super.onDestroy();
    }
    @Override
    public void onDestroyView() {
        client.cancelRequests(this.getActivity(),true);
        super.onDestroyView();
    }

    public void loadData()
    {
        pDialog = new SweetAlertDialog(this.getActivity(), SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        pDialog.setTitleText("Cargando...");
        client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        Location loc;
        if(this.getActivity().getLocalClassName().equals("MainActivity")) {
            loc = ((MainActivity) getActivity()).GetLocationSelected();
        }else{
            loc = ((MainActivityUser) getActivity()).GetLocationSelected();
        }
        params.put("id", loc.getId());

        String url=Links.getUrl_get_images_location();

        client.post(url,params,new AsyncHttpResponseHandler() {
            @Override
            public void onStart()
            {
                pDialog.setCancelable(true);
                pDialog.show();
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if(statusCode==200)
                {

                    try {
                        System.out.println(new String(responseBody));
                        setResult(new String(responseBody));
                        layout.setRefreshing(false);


                    }catch(JSONException e)
                    {
                        System.out.println("Falla:"+e );
                        layout.setRefreshing(false);
                    }
                }
                pDialog.hide();
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                pDialog.hide();
                layout.setRefreshing(false);
            }
        });

    }

    public ArrayList setResult (String result) throws JSONException {

        list.clear();
        String cadenaJSON = result.toString();//Le pasamos a la variable cadenaJSON una cadena de tipo JSON (en este caso es la creada anteriormente)

        JSONObject jsonObject = new JSONObject(cadenaJSON); //Creamos un objeto de tipo JSON y le pasamos la cadena JSON
        String posts = jsonObject.getString("posts");

        JSONArray array = new JSONArray(posts);
        for(int i=0; i < array.length(); i++) {
            JSONObject jsonChildNode = array.getJSONObject(i);
            jsonChildNode = new JSONObject(jsonChildNode.optString("post").toString());

            ArrayList<String> datos = new ArrayList<String>();
            datos.add(jsonChildNode.getString("photo_url"));
            datos.add(jsonChildNode.getString("id"));
            datos.add(jsonChildNode.getString("rating"));
            list.add(datos);
            urls.add(jsonChildNode.getString("photo_url"));
        }
        gridAdapter.changeModelList(urls,list);
        fab.attachToListView(gv);
        return  list;
    }


    public void ViewFull()
    {

        FragmentImageViewer dFragment = new FragmentImageViewer();
        dFragment.setTargetFragment(this, 0);
        // Show DialogFragment
        Class cl=this.getActivity().getClass();
        if(cl.getName().equals("world.interesting.panche.interestingworld.MainActivity")) {
            ((MainActivity) getActivity()).SetImageUrlFull(select_image.get(0));
        }else {
            ((MainActivityUser) getActivity()).SetImageUrlFull(select_image.get(0),select_image.get(1));
        }
        dFragment.show(fm, "Dialog Photo");

    }


    public void selectImage (View view)
    {

        final String[] option = new String[] { "Hacer una fotografía", "Buscar una fotografía"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.select_dialog_item, option);
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setTitle("Elige una opción");
        builder.setAdapter(adapter, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which) {
                switch (which)
                {
                    case 0:
                        take_photo();
                        break;
                    case 1:
                        image_path();
                        break;
                    default:
                        break;
                }

            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();

    }
    //Seleccionar una imagen por la ruta
    public void image_path()
    {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        int  code = 2;
        startActivityForResult(intent, code);
    }
    //Realizar una fotografía nueva
    public void take_photo()
    {
        Intent imageIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        //folder stuff
        File imagesFolder = new File(Environment.getExternalStorageDirectory(), "Interesting");
        imagesFolder.mkdirs();

        image = new File(imagesFolder, "QR_" + timeStamp + ".png");
        selectedImage = Uri.fromFile(image);

        imageIntent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImage);
        startActivityForResult(imageIntent, 1);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1) {
                try {

                    is = this.getActivity().getContentResolver().openInputStream(selectedImage);
                    InputStream is2 = this.getActivity().getContentResolver().openInputStream(selectedImage);
                    BufferedInputStream bis = new BufferedInputStream(is);
                    BufferedInputStream bis2 = new BufferedInputStream(is2);


                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(bis, null, options);
                    //Comprimimos la imagen
                    options.inSampleSize = calculateInSampleSize(options, 480, 480);
                    options.inJustDecodeBounds = false;
                    Bitmap bitmap = BitmapFactory.decodeStream(bis2, null, options);

                    //Rotar la imagen
                    String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
                    Cursor cur = this.getActivity().getContentResolver().query(selectedImage, orientationColumn, null, null, null);
                    Matrix matrix = new Matrix();
                    int orientation = -1;
                    if (cur != null && cur.moveToFirst()) {
                        orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));

                        matrix.postRotate(orientation);
                    }
                    else
                    {
                        try {
                            ExifInterface exif = new ExifInterface(
                                    image.getAbsolutePath());    //Since API Level 5
                            String exifOrientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
                            orientation = exif.getAttributeInt(
                                    ExifInterface.TAG_ORIENTATION,
                                    ExifInterface.ORIENTATION_NORMAL);
                            int rotate = 0;
                            switch (orientation) {
                                case ExifInterface.ORIENTATION_ROTATE_270:
                                    rotate = 270;
                                    break;
                                case ExifInterface.ORIENTATION_ROTATE_180:
                                    rotate = 180;
                                    break;
                                case ExifInterface.ORIENTATION_ROTATE_90:
                                    rotate = 90;
                                    break;

                            }
                            matrix.postRotate(rotate);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream);

                    is = new ByteArrayInputStream(stream.toByteArray());
                    System.out.println(bitmap.getHeight() + " " + bitmap.getWidth());
                    SweetAlert();

                } catch (FileNotFoundException e) {
                    System.out.println("Error: " + e);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (requestCode == 2) {

                try {
                    if (data != null) {

                        selectedImage = data.getData();
                        is = this.getActivity().getContentResolver().openInputStream(selectedImage);
                        InputStream is2 = this.getActivity().getContentResolver().openInputStream(selectedImage);
                        BufferedInputStream bis = new BufferedInputStream(is);
                        BufferedInputStream bis2 = new BufferedInputStream(is2);


                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(bis, null, options);
                        //Comprimimos la imagen
                        options.inSampleSize = calculateInSampleSize(options, 480, 480);
                        options.inJustDecodeBounds = false;
                        Bitmap bitmap = BitmapFactory.decodeStream(bis2, null, options);

                        //Rotar la imagen
                        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
                        Cursor cur = this.getActivity().getContentResolver().query(selectedImage, orientationColumn, null, null, null);
                        int orientation = -1;
                        if (cur != null && cur.moveToFirst()) {
                            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
                        }
                        Matrix matrix = new Matrix();
                        matrix.postRotate(orientation);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream);

                        is = new ByteArrayInputStream(stream.toByteArray());
                        System.out.println(bitmap.getHeight() + " " + bitmap.getWidth());
                        SweetAlert();

                    }
                }catch (FileNotFoundException e) {
                    new SweetAlertDialog(this.getActivity(), SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Oops...")
                            .setContentText("Parece que algo ha fallado!")
                            .show();
                } catch (IOException e) {
                    new SweetAlertDialog(this.getActivity(), SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Oops...")
                            .setContentText("Parece que algo ha fallado!")
                            .show();
                }

            }
        }
    }
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height;
            final int halfWidth = width;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize++;
            }
        }

        return inSampleSize;
    }

    public void SweetAlert()
    {
        new SweetAlertDialog(this.getActivity(), SweetAlertDialog.WARNING_TYPE)
                .setTitleText("")
                .setContentText("Deseas subir la fotografía?")
                .setCancelText("No")
                .setConfirmText("Sí")
                .showCancelButton(true)
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                    }
                })
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                        try {
                            buttonAccept();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .show();
    }
    public void buttonAccept() throws JSONException, FileNotFoundException {
        //Inicializamos dialog
        pDialog = new SweetAlertDialog(this.getActivity());
        pDialog = new SweetAlertDialog(this.getActivity(), SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        pDialog.setTitleText("Cargando...");

        datos=Preferences.loadPreferences(this.getActivity());

        result="";

        client = new AsyncHttpClient();


        String url = Links.getUrl_add_image_location();
        RequestParams params = new RequestParams();
        params.put("id_user", datos[0]);
        params.put("id_location", id);

        //Cargar la imagen
        int numero1 = (int) (Math.random() * 99999999) + 1;
        int numero2 = (int) (Math.random() * 99999999) + 1;
        int numero3 = (int) (Math.random() * 99999999) + 1;
        params.put("photo_url", is, numero1+""+numero2+""+numero3 + "_location.jpg");

        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {

            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if (statusCode == 200) {

                    try {
                        System.out.println(new String(responseBody));
                        setResult2(new String(responseBody));
                        System.out.println(getResult());
                        if (getResult().equals("bien")) {
                            AppMsg.makeText(FragmentPhotosDetail.this.getActivity(), "Imagen subida correctamente", AppMsg.STYLE_INFO).setLayoutGravity(Gravity.BOTTOM).show();

                            urls.clear();
                            loadData();
                        } else {
                            AppMsg.makeText(FragmentPhotosDetail.this.getActivity(), "Error al intentar subir la imágen", AppMsg.STYLE_ALERT).setLayoutGravity(Gravity.BOTTOM).show();
                        }

                    } catch (JSONException e) {
                        System.out.println("Falla:" + e);
                        AppMsg.makeText(FragmentPhotosDetail.this.getActivity(), "Error al intentar subir la imágen", AppMsg.STYLE_ALERT).setLayoutGravity(Gravity.BOTTOM).show();

                    }
                }
                pDialog.hide();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                AppMsg.makeText(FragmentPhotosDetail.this.getActivity(), "Parece que hay algún problema con la red", AppMsg.STYLE_CONFIRM).setLayoutGravity(Gravity.BOTTOM).show();
                pDialog.hide();
            }
        });
    }

    public String getResult (){


        return this.result;
    }
    public String setResult2 (String result) throws JSONException {
        String cadenaJSON = result.toString();//Le pasamos a la variable cadenaJSON una cadena de tipo JSON (en este caso es la creada anteriormente)

        JSONObject jsonObject = new JSONObject(cadenaJSON); //Creamos un objeto de tipo JSON y le pasamos la cadena JSON
        String posts = jsonObject.getString("posts");

        //Entramos en el array de posts
        jsonObject=new JSONObject(posts);

        //A través de los nombres de cada dato obtenemos su contenido
        return  this.result=jsonObject.getString("Estado").toLowerCase();
    }



    @Override
    public void onRateImage(String State) {
        urls.clear();
        loadData();
    }
}
