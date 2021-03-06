package com.zero.admin.util;

import com.zero.common.util.StringHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.zero.common.constants.SystemConstants.COOKIE_NAME;
import static com.zero.common.constants.SystemConstants.DEFAULT_MAX_AGE;

/**
 *
 * @description 验证码处理类--需要注意linux下是否存在字体,否则会报错
 * @author yezhaoxing
 * @date 2017/11/02
 */
@Configuration
@Slf4j
public class CaptchaUtil {

    private static final long CAPTCHA_EXPIRED_SECONDS = ((Long) TimeUnit.MINUTES.toSeconds(5)).intValue();
    // 默认的验证码大小
    private static final int WIDTH = 108, HEIGHT = 40, CODE_SIZE = 4;
    // 验证码随机字符数组
    private static final char[] CHAR_ARRAY = "3456789ABCDEFGHJKMNPQRSTUVWXY".toCharArray();
    // 验证码字体
    private static final Font[] RANDOM_FONT = new Font[] { new Font("nyala", Font.BOLD, 38),
            new Font("Arial", Font.BOLD, 32), new Font("Bell MT", Font.BOLD, 32),
            new Font("Credit valley", Font.BOLD, 34), new Font("Impact", Font.BOLD, 32),
            new Font(Font.MONOSPACED, Font.BOLD, 40) };
    // 生成随机类
    private static final Random RANDOM = new Random();
    @Resource
    private RedisHelper<String, String> redisHelper;

    private static void drawGraphic(BufferedImage image, String code) {
        // 获取图形上下文
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        // 图形抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 字体抗锯齿
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 设定背景色，淡色
        g.setColor(getRandColor(210, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 画小字符背景
        Color color = null;
        for (int i = 0; i < 20; i++) {
            color = getRandColor(120, 200);
            g.setColor(color);
            String rand = String.valueOf(CHAR_ARRAY[RANDOM.nextInt(CHAR_ARRAY.length)]);
            g.drawString(rand, RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
            color = null;
        }
        // 取随机产生的认证码(4位数字)
        char[] buffer = code.toCharArray();
        for (int i = 0; i < buffer.length; i++) {
            char charCode = buffer[i];
            // 旋转度数 最好小于45度
            int degree = RANDOM.nextInt(28);
            if (i % 2 == 0) {
                degree = degree * (-1);
            }
            // 定义坐标
            int x = 22 * i, y = 21;
            // 旋转区域
            g.rotate(Math.toRadians(degree), x, y);
            // 设定字体颜色
            color = getRandColor(20, 130);
            g.setColor(color);
            // 设定字体，每次随机
            g.setFont(RANDOM_FONT[RANDOM.nextInt(RANDOM_FONT.length)]);
            // 将认证码显示到图象中
            g.drawString("" + charCode, x + 8, y + 10);
            // 旋转之后，必须旋转回来
            g.rotate(-Math.toRadians(degree), x, y);
        }
        // 图片中间曲线，使用上面缓存的color
        g.setColor(color);
        // width是线宽,float型
        BasicStroke bs = new BasicStroke(3);
        g.setStroke(bs);
        // 画出曲线
        QuadCurve2D.Double curve = new QuadCurve2D.Double(0d, RANDOM.nextInt(HEIGHT - 8) + 4, WIDTH / 2, HEIGHT / 2,
                WIDTH, RANDOM.nextInt(HEIGHT - 8) + 4);
        g.draw(curve);
        // 销毁图像
        g.dispose();
    }

    /**
     * 给定范围获得随机颜色
     */
    private static Color getRandColor(int fc, int bc) {
        if (fc > 255) {
            fc = 255;
        }
        if (bc > 255) {
            bc = 255;
        }
        int r = fc + RANDOM.nextInt(bc - fc);
        int g = fc + RANDOM.nextInt(bc - fc);
        int b = fc + RANDOM.nextInt(bc - fc);
        return new Color(r, g, b);
    }

    /**
     * 生成验证码
     */
    public void generate(HttpServletRequest request, HttpServletResponse response) {
        // 先检查cookie的uuid是否存在
        String cookieValue = WebHelper.getCookieValue(request, COOKIE_NAME);
        if (StringUtils.isEmpty(cookieValue)) {
            cookieValue = StringHelper.generateUUId();
            WebHelper.setCookie(response, COOKIE_NAME, cookieValue, DEFAULT_MAX_AGE);
        }
        String captchaCode = this.generateCode().toUpperCase();// 转成大写重要
        // 生成验证码
        generate(response, captchaCode);
        redisHelper.set(wrapperRedisKey(cookieValue), captchaCode, CAPTCHA_EXPIRED_SECONDS);
    }

    /**
     * 仅能验证一次，验证后立即删除
     */
    public boolean validate(HttpServletRequest request, String userInputCaptcha) {
        String cookieValue = WebHelper.getCookieValue(request, COOKIE_NAME);
        if (StringUtils.isEmpty(cookieValue)) {
            return false;
        }
        String redisKey = wrapperRedisKey(cookieValue);
        String captchaCode = redisHelper.get(redisKey);
        if (StringUtils.isEmpty(captchaCode)) {
            return false;
        }
        // 转成大写重要
        userInputCaptcha = userInputCaptcha.toUpperCase();
        boolean result = userInputCaptcha.equals(captchaCode);
        if (result) {
            redisHelper.delete(redisKey);
        }
        return result;
    }

    private String wrapperRedisKey(String sessionId) {
        return String.format("%s-%s", "captcha", sessionId);
    }

    /**
     * 生成验证码
     */
    private void generate(HttpServletResponse response, String vCode) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");

        ServletOutputStream sos = null;
        try {
            drawGraphic(image, vCode);
            sos = response.getOutputStream();
            ImageIO.write(image, "JPEG", sos);
            sos.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (sos != null) {
                    sos.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    /**
     * 生成验证码字符串
     *
     * @return 验证码字符串
     */
    private String generateCode() {
        int count = 4;
        char[] buffer = new char[count];
        for (int i = 0; i < count; i++) {
            buffer[i] = CHAR_ARRAY[RANDOM.nextInt(CHAR_ARRAY.length)];
        }
        return new String(buffer);
    }
}
