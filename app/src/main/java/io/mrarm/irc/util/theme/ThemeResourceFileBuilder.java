package io.mrarm.irc.util.theme;

import android.content.Context;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.mrarm.arsc.ArscWriter;
import io.mrarm.arsc.chunks.ResTable;
import io.mrarm.arsc.chunks.ResValue;
import io.mrarm.irc.R;

public class ThemeResourceFileBuilder {

    private static int sessionThemeIndex = 0;

    private static void setUseLightActionBar(ResTable.MapEntry theme) {
        theme.addValue(R.attr.actionBarPopupTheme,
                new ResValue.Reference(R.style.ThemeOverlay_AppCompat));
        theme.addValue(R.attr.actionBarTheme,
                new ResValue.Reference(R.style.ThemeOverlay_AppCompat_ActionBar));
    }

    public static CustomTheme createTheme(Context ctx, ThemeInfo theme) {
        ResTable table = new ResTable();
        ResTable.Package pkg = new ResTable.Package(0x7e, "io.mrarm.irc.theme");

        ResTable.TypeSpec colorTypeSpec = new ResTable.TypeSpec(1, "color",
                new int[theme.colors.size()] /* should be filled with zeros */);
        pkg.addType(colorTypeSpec);
        ResTable.Type colorType = new ResTable.Type(1, new ResTable.Config());
        List<String> colors = new ArrayList<>();
        for (Map.Entry<String, Integer> p : theme.colors.entrySet()) {
            ResTable.Entry colorPrimary = new ResTable.Entry(colors.size(), p.getKey(),
                    new ResValue.Integer(ResValue.TYPE_INT_COLOR_ARGB8, p.getValue()));
            colorType.addEntry(colorPrimary);
            colors.add(p.getKey());
        }
        pkg.addType(colorType);

        ResTable.TypeSpec styleTypeSpec = new ResTable.TypeSpec(2, "style", new int[] { 0, 0 });
        pkg.addType(styleTypeSpec);
        ResTable.Type styleType = new ResTable.Type(2, new ResTable.Config());
        ResTable.MapEntry appTheme = new ResTable.MapEntry(0, "AppTheme");
        appTheme.setParent(theme.baseThemeInfo.getThemeResId());
        styleType.addEntry(appTheme);
        ResTable.MapEntry appThemeNoActionBar = new ResTable.MapEntry(1, "AppTheme.NoActionBar");
        appThemeNoActionBar.setParent(theme.baseThemeInfo.getThemeNoActionBarResId());
        styleType.addEntry(appThemeNoActionBar);
        pkg.addType(styleType);

        for (int i = 0; i < colors.size(); i++) {
            String color = colors.get(i);
            List<Integer> resIds = ThemeAttrMapping.getColorAttrs(color);
            if (resIds == null)
                continue;
            for (int resId : resIds)
                appTheme.addValue(resId, new ResValue.Reference(pkg, colorTypeSpec, i));
            for (int resId : resIds)
                appThemeNoActionBar.addValue(resId, new ResValue.Reference(pkg, colorTypeSpec, i));
        }
        if (theme.getBool(ThemeInfo.PROP_LIGHT_TOOLBAR) == Boolean.TRUE) {
            setUseLightActionBar(appTheme);
            setUseLightActionBar(appThemeNoActionBar);
        }
        Collections.sort(appTheme.value, (a, b) -> a.name - b.name);
        Collections.sort(appThemeNoActionBar.value, (a, b) -> a.name - b.name);

        table.addPackage(pkg);
        return new CustomTheme(ResTable.makeReference(pkg, styleTypeSpec, appTheme),
                ResTable.makeReference(pkg, styleTypeSpec, appThemeNoActionBar), table);
    }

    private static void buildThemeZipFile(File zipPath, ResTable resTable) {
        try {
            FileOutputStream fos = new FileOutputStream(zipPath);
            ZipOutputStream outStream = new ZipOutputStream(new BufferedOutputStream(fos));
            outStream.setMethod(ZipOutputStream.STORED);

            ByteArrayOutputStream byteArrOS = new ByteArrayOutputStream();
            ArscWriter writer = new ArscWriter(resTable);
            writer.write(byteArrOS);
            byte[] bytes = byteArrOS.toByteArray();

            ZipEntry entry = new ZipEntry("resources.arsc");
            entry.setSize(bytes.length);
            CRC32 crc = new CRC32();
            crc.update(bytes);
            entry.setCrc(crc.getValue());
            outStream.putNextEntry(entry);
            outStream.write(bytes);

            outStream.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File getThemesDir(Context context) {
        return new File(context.getFilesDir(), "themes/cache");
    }

    private static void deleteOldThemeFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null)
            return;
        for (File file : files)
            file.delete();
    }

    public static File createThemeZipFile(Context context, ResTable resTable) {
        File dir = getThemesDir(context);
        dir.mkdirs();
        deleteOldThemeFiles(dir);
        File file = new File(dir, "theme." + sessionThemeIndex +  ".zip");
        buildThemeZipFile(file, resTable);
        ++sessionThemeIndex;
        return file;
    }


    public static class CustomTheme extends ThemeManager.ThemeResInfo {

        private ResTable resTable;

        public CustomTheme(int themeResId, int themeNoActionBarResId, ResTable resTable) {
            super(themeResId, themeNoActionBarResId);
            this.resTable = resTable;
        }

        public ResTable getResTable() {
            return resTable;
        }

    }

}
