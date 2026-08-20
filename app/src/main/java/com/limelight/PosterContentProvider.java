package com.limelight;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.limelight.grid.assets.DiskAssetLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class PosterContentProvider extends ContentProvider {


    /*
     * O authority sai do pacote EM TEMPO DE EXECUCAO, e nao de
     * BuildConfig.APPLICATION_ID.
     *
     * Motivo: este modulo virou biblioteca (o MugenTerminal embute o Moonlight
     * em vez de abrir outro aplicativo), e APPLICATION_ID so existe no
     * BuildConfig de um APLICATIVO. O manifest ja declarava o provider como
     * "poster.${applicationId}", que a mesclagem resolve com o pacote de quem
     * consome; aqui do lado do Java o valor tem que chegar pelo contexto, senao
     * o authority declarado e o usado deixam de casar e o boxart para de
     * carregar sem dizer por que.
     */
    private static volatile String sAuthority;
    public static final String PNG_MIME_TYPE = "image/png";
    public static final int APP_ID_PATH_INDEX = 2;
    public static final int COMPUTER_UUID_PATH_INDEX = 1;
    private DiskAssetLoader mDiskAssetLoader;

    private static final UriMatcher sUriMatcher;
    private static final String BOXART_PATH = "boxart";
    private static final int BOXART_URI_ID = 1;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        int match = sUriMatcher.match(uri);
        if (match == BOXART_URI_ID) {
            return openBoxArtFile(uri, mode);
        }
        return openBoxArtFile(uri, mode);

    }

    public ParcelFileDescriptor openBoxArtFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new UnsupportedOperationException("This provider is only for read mode");
        }

        List<String> segments = uri.getPathSegments();
        if (segments.size() != 3) {
            throw new FileNotFoundException();
        }
        String appId = segments.get(APP_ID_PATH_INDEX);
        String uuid = segments.get(COMPUTER_UUID_PATH_INDEX);
        File file = mDiskAssetLoader.getFile(uuid, Integer.parseInt(appId));
        if (file.exists()) {
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        throw new FileNotFoundException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("This provider is only for read mode");
    }

    @Override
    public String getType(Uri uri) {
        return PNG_MIME_TYPE;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("This provider is only for read mode");
    }

    @Override
    public boolean onCreate() {
        mDiskAssetLoader = new DiskAssetLoader(getContext());
        // O provider nasce junto com o processo, antes de qualquer tela: e o
        // primeiro momento em que da pra saber o pacote de quem esta usando.
        sAuthority = "poster." + getContext().getPackageName();
        sUriMatcher.addURI(sAuthority, BOXART_PATH, BOXART_URI_ID);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("This provider doesn't support query");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException("This provider is support read only");
    }


    public static Uri createBoxArtUri(String uuid, String appId) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(sAuthority)
                .appendPath(BOXART_PATH)
                .appendPath(uuid)
                .appendPath(appId)
                .build();
    }

}
