package com.medicine.sales.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medicine.sales.entity.BannerInfo;
import com.medicine.sales.mapper.BannerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 轮播图默认图片初始化：为使用占位路径的轮播图生成本地图片
 */
@Slf4j
@Component
@Order(2)
public class BannerImageInitializer implements CommandLineRunner {

    @Value("${upload.path}")
    private String uploadPath;

    @Resource
    private BannerMapper bannerMapper;

    private static final int IMG_WIDTH = 1200;
    private static final int IMG_HEIGHT = 400;

    @Override
    public void run(String... args) {
        List<BannerInfo> banners = bannerMapper.selectList(
                new LambdaQueryWrapper<BannerInfo>()
                        .like(BannerInfo::getImage, "/images/"));
        if (banners.isEmpty()) {
            return;
        }
        log.info("Found {} banner(s) with placeholder images, generating local images...", banners.size());

        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Color[] colors = {
                new Color(255, 122, 69),   // #FF7A45 主色
                new Color(82, 196, 26),    // #52C41A 绿色
                new Color(24, 144, 255)    // #1890FF 蓝色
        };

        for (int i = 0; i < banners.size(); i++) {
            BannerInfo banner = banners.get(i);
            try {
                String filename = "banner-default-" + banner.getId() + ".png";
                File outFile = new File(dir, filename);
                if (outFile.exists()) {
                    banner.setImage("/uploads/" + filename);
                    bannerMapper.updateById(banner);
                    continue;
                }
                generateBannerImage(banner.getTitle(), colors[i % colors.length], outFile);
                banner.setImage("/uploads/" + filename);
                bannerMapper.updateById(banner);
                log.info("Generated banner image: {} -> {}", banner.getTitle(), filename);
            } catch (Exception e) {
                log.warn("Failed to generate banner image for id={}: {}", banner.getId(), e.getMessage());
            }
        }
    }

    private void generateBannerImage(String title, Color themeColor, File outFile) throws IOException {
        BufferedImage img = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 渐变背景
        GradientPaint gradient = new GradientPaint(0, 0, themeColor.brighter(),
                IMG_WIDTH, IMG_HEIGHT, themeColor.darker());
        g.setPaint(gradient);
        g.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);

        // 半透明遮罩
        g.setColor(new Color(0, 0, 0, 30));
        g.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);

        // 标题文字（使用系统默认字体）
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(title);
        int x = Math.max(20, (IMG_WIDTH - textWidth) / 2);
        int y = IMG_HEIGHT / 2 + fm.getAscent() / 2 - 10;
        g.drawString(title, x, y);

        // 副标题
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        g.setColor(new Color(255, 255, 255, 200));
        String sub = "Medicine Sales Platform";
        fm = g.getFontMetrics();
        int subWidth = fm.stringWidth(sub);
        g.drawString(sub, (IMG_WIDTH - subWidth) / 2, y + 50);

        g.dispose();
        ImageIO.write(img, "png", outFile);
    }
}
