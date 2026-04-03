package com.DamnLol.ChasmSigil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class ChasmSigilOverlay extends Overlay
{
    // Base layout
    private static final float BASE_ICON = 62f;
    private static final float BASE_PAD = 8f;
    private static final float BASE_TEXT_W = 150f;
    private static final float BASE_W = BASE_PAD + BASE_ICON + BASE_PAD + BASE_TEXT_W + BASE_PAD + BASE_ICON + BASE_PAD;
    private static final float BASE_H = BASE_ICON + BASE_PAD * 2f;
    private static final float BASE_NAME = 15f;
    private static final float BASE_DESC = 13f;
    private static final float BASE_CNT = 20f;

    private static final int YELLOW_AT = 32;
    private static final int RED_AT = 65;

    private static final Color BG = new Color(20, 4, 4, 220);
    private static final Color BORDER = new Color(100, 20, 20, 255);
    private static final Color NAME_COL = new Color(175, 135, 2);
    private static final Color DESC_COL = new Color( 70, 210, 210);
    private static final Color COUNT_GREEN = new Color(0, 210, 0);
    private static final Color COUNT_YELLOW = new Color(255, 210, 0);
    private static final Color COUNT_RED = new Color(220, 30, 30);

    private final ChasmSigilPlugin plugin;

    private final BufferedImage[] sigilImages = new BufferedImage[Sigil.values().length];
    private BufferedImage counterBg = null;

    @Inject
    public ChasmSigilOverlay(ChasmSigilPlugin plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.MED);
        setMovable(true);
        setResizable(true);
        setSnappable(true);
        setMinimumSize(160);
        setPreferredSize(new Dimension((int) BASE_W, (int) BASE_H));
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        if (!plugin.isInChasm()) return null;

        Sigil sigil = plugin.getActiveSigil();
        int killCount = plugin.getKillCount();


        if (sigil == Sigil.UNKNOWN) return null;

        // Scale everything uniformly
        Dimension pref = getPreferredSize();
        int prefW = (pref != null && pref.width > 0) ? pref.width : (int) BASE_W;
        float s = prefW / BASE_W;

        int icon = round(BASE_ICON * s);
        int pad = round(BASE_PAD * s);
        int width = round(BASE_W * s);
        int height = round(BASE_H * s);
        int nameSz = Math.max(8, round(BASE_NAME * s));
        int descSz = Math.max(7, round(BASE_DESC * s));
        int cntSz = Math.max(9, round(BASE_CNT * s));

        setPreferredSize(new Dimension(width, height));

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g.setColor(BG);
        g.fillRoundRect(0, 0, width, height, 6, 6);
        g.setColor(BORDER);
        g.drawRoundRect(0, 0, width - 1, height - 1, 6, 6);

        int iconY = (height - icon) / 2;

        int sigilX = pad;
        int counterX = width - pad - icon;
        int textLeft = sigilX + icon + pad;
        int textW = counterX - pad - textLeft;

        // sigil
        BufferedImage sigImg = getSigilImage(sigil.ordinal());
        if (sigImg != null)
            g.drawImage(sigImg, sigilX, iconY, icon, icon, null);

        if (counterBg == null)
            counterBg = loadImage("/counter.png");
        if (counterBg != null)
            g.drawImage(counterBg, counterX, iconY, icon, icon, null);

        String str = String.valueOf(Math.min(killCount, 999));
        int sz = str.length() >= 3 ? Math.max(8, round(cntSz * 0.75f)) : cntSz;
        Font cntFont = new Font("Arial", Font.BOLD, sz);
        g.setFont(cntFont);
        FontMetrics fm = g.getFontMetrics();
        Color col = killCount >= RED_AT ? COUNT_RED
                : killCount >= YELLOW_AT ? COUNT_YELLOW
                : COUNT_GREEN;
        java.awt.geom.Rectangle2D bounds = fm.getStringBounds(str, g);
        int numX = counterX + (int) Math.round((icon - bounds.getWidth()) / 2.0);
        int numY = iconY + (icon + fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(Color.BLACK);
        g.drawString(str, numX + 1, numY + 1);
        g.setColor(col);
        g.drawString(str, numX, numY);

        String name = sigil.getContractName();
        String desc = sigil.getDescription();

        // Contract name
        Font nameFont = new Font("Arial", Font.BOLD, nameSz);
        g.setFont(nameFont);
        g.setColor(NAME_COL);
        FontMetrics nameFm = g.getFontMetrics();
        int nameX = textLeft + (textW - nameFm.stringWidth(name)) / 2;
        int nameY = pad + nameFm.getAscent();
        g.drawString(name, nameX, nameY);
        g.setStroke(new BasicStroke(1f));
        g.drawLine(nameX, nameY + 2, nameX + nameFm.stringWidth(name), nameY + 2);

        // Kill style
        Font descFont = new Font("Arial", Font.PLAIN, descSz);
        g.setFont(descFont);
        g.setColor(DESC_COL);
        FontMetrics descFm = g.getFontMetrics();
        int descX = textLeft + (textW - descFm.stringWidth(desc)) / 2;
        int descY = height - pad - descFm.getDescent() - round(height * 0.08f);
        g.drawString(desc, descX, descY);

        return new Dimension(width, height);
    }

    public void resetSize()
    {
        setPreferredSize(new Dimension((int) BASE_W, (int) BASE_H));
    }

    private static int round(float v) { return Math.round(v); }

    private BufferedImage getSigilImage(int ord)
    {
        if (sigilImages[ord] == null)
            sigilImages[ord] = loadImage("/sigil" + (ord + 1) + ".png");
        return sigilImages[ord];
    }

    private BufferedImage loadImage(String path)
    {
        try (InputStream in = getClass().getResourceAsStream(path))
        {
            if (in == null) return null;
            return ImageIO.read(in);
        }
        catch (IOException e)
        {
            return null;
        }
    }
}