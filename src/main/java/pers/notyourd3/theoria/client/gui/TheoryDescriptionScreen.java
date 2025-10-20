package pers.notyourd3.theoria.client.gui;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import pers.notyourd3.theoria.theory.Theory;


public class TheoryDescriptionScreen extends Screen {
    // 资源图片的总尺寸（用于纹理坐标计算）
    private static final int TEX_FULL_WIDTH = 256;
    private static final int TEX_FULL_HEIGHT = 256;

    // 1. 定义源纹理（Source Texture）的裁剪区域和边框
    private static final int SRC_X = 0;              // 源纹理起始X坐标 (从左上角开始)
    private static final int SRC_Y = 0;              // 源纹理起始Y坐标
    private static final int SRC_WIDTH = 248;        // 源纹理总宽度 (248)
    private static final int SRC_HEIGHT = 166;       // 源纹理总高度 (166)
    private static final int BORDER_SIZE = 4;        // 边框宽度 (4 像素)


    private static final String NORMAL="597BA6";
    private static final String EPIC="724A8B";
    private static final String LEGENDARY="3E8B77";
    private static final String UNIQUE = "EB5E28";

    private final Theory theory;
    private final Screen parent; // 用于返回之前的屏幕

    public TheoryDescriptionScreen(Theory theory,Screen parent){
        super(Component.literal("detail"));
        this.theory = theory;
        this.parent = parent;
    }
    @Override
    protected void init() {
        super.init();
        // 添加一个返回按钮
        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 渲染父屏幕作为背景，实现淡出效果
        if (this.parent != null) {
            this.parent.render(graphics, -1, -1, partialTick);
        }
        // 添加一个半透明遮罩
        //graphics.fill(0, 0, this.width, this.height, 0x90000000);

        super.render(graphics, mouseX, mouseY, partialTick);
        renderProportionalTallPanel(graphics,ResourceLocation.withDefaultNamespace("textures/gui/demo_background.png"), width,height);
        int contentWidth = this.width - 80;
        int xPos = 40;
        int yPos = 40;

        // 绘制标题
       // graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // 绘制图标
        //graphics.blit(theory.icon(), xPos, yPos, 0, 0, 32, 32, 32, 32);

        // 绘制副标题
       // graphics.drawString(this.font, theory.subTitle(), xPos + 40, yPos + 12, 0xAAAAAA);
        yPos += 40;

        // 使用 `drawWordWrap` 自动换行渲染描述文本
        // `screens.md` 中提到了这个方便的方法
       // graphics.drawWordWrap(this.font, theory.description(), xPos, yPos, contentWidth, 0xFFFFFF);
    }
    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    public void renderProportionalTallPanel(
            GuiGraphics graphics,
            ResourceLocation texture,
            int screenWidth, int screenHeight
    ) {
        // 1. 定义容器（理论树区域的边界）
        int containerX = 0;
        int containerY = 0;
        int containerW = screenWidth;
        int containerH = screenHeight-50;

        // 2. 确定最大的可容纳尺寸 (保持 TARGET_ASPECT_RATIO)

        // A. 尝试以容器的高度为基准计算宽度
        double widthBasedOnH = containerH * 0.8;

        // B. 检查宽度是否超出容器
        int finalW, finalH;

        if (widthBasedOnH <= containerW) {
            // 宽度符合要求，高度是限制因素 (Tall Panel模式)
            finalH = containerH;
            finalW = (int) widthBasedOnH;
        } else {
            // 宽度超出容器，宽度是限制因素
            finalW = containerW;
            finalH = (int) (containerW / 0.8);
        }

        // 3. 计算居中位置
        // 如果 finalW/finalH 小于 containerW/containerH，则需要居中
        int finalX = containerX + (containerW - finalW) / 2;
        int finalY = containerY + (containerH - finalH) / 2;
        int rgbColor = Integer.parseInt(theory.color(),16);
        int argbColor = 0xFF000000 | rgbColor;

        renderNineSlice(
                graphics,
                texture,
                finalX, finalY,       // 居中后的 X, Y
                finalW, finalH        // 等比例计算后的 W, H
        );
        graphics.fill(finalX+4,finalY+4,finalX+finalW-4,(int)(finalY+finalH/2.2),
                argbColor
        );
        graphics.pose().pushMatrix();
        graphics.pose().scale(2f);
        graphics.drawCenteredString(font,theory.title(),(finalX+finalW/2)/2,(finalY+finalH/7/+5)/2,0xFFFFFFFF);
        graphics.pose().scale(2f);
        int iconX = (finalX+finalW/2-32)/4;
        int iconY = (finalY+finalH/4-24)/4;
        if(theory.icon().getPath().contains(".png")){
            graphics.blit(theory.icon(),
                    iconX, iconY, iconX+16,iconY+16, 0, 1, 0, 1);
        }else{
            graphics.renderItem(Item.byId(BuiltInRegistries.ITEM.getId(theory.icon())).getDefaultInstance(), iconX, iconY);
        }
        graphics.pose().popMatrix();
        graphics.drawCenteredString(this.font, theory.subTitle(), this.width / 2, 22, 0xFFFFFFFF);
        graphics.drawWordWrap(font, theory.description(), finalX+8, finalY+finalH/2+8, finalW-16, 0xFFFFFFFF);
    }
    public static void renderNineSlice(GuiGraphics graphics, ResourceLocation texture, int x, int y, int targetWidth, int targetHeight) {

        // 【计算屏幕上中心区域的尺寸】
        // 目标中心区域宽度 = 目标总宽度 - 2 * 边框宽度
        int targetCenterWidth = targetWidth - 2 * BORDER_SIZE;
        int targetCenterHeight = targetHeight - 2 * BORDER_SIZE;

        // 如果目标尺寸小于边框，则无法正确渲染，此处可添加错误处理或裁剪逻辑。
        if (targetCenterWidth < 0 || targetCenterHeight < 0) {
            // 简单处理：如果太小，则不渲染或只渲染最小边框
            return;
        }

        // 【计算源纹理中心区域的尺寸】
        // 源中心区域宽度 = 源纹理总宽度 - 2 * 边框宽度
        int srcCenterWidth = SRC_WIDTH - 2 * BORDER_SIZE;  // 240
        int srcCenterHeight = SRC_HEIGHT - 2 * BORDER_SIZE; // 158

        // 定义渲染的 9 个区域的 X/Y 坐标和 U/V 坐标

        // S: 屏幕 X 坐标的四个关键点
        // S_0: 起始 X (x)
        // S_1: 左边框结束 (x + 4)
        // S_2: 中心区域结束 (x + 4 + targetCenterWidth)
        // S_3: 目标结束 (x + targetWidth)
        int[] sx = {x, x + BORDER_SIZE, x + BORDER_SIZE + targetCenterWidth, x + targetWidth};

        // T: 纹理 X 坐标的四个关键点 (像素值)
        // T_0: 源起始 X (0)
        // T_1: 左边框结束 (0 + 4)
        // T_2: 中心区域结束 (0 + 4 + srcCenterWidth)
        // T_3: 源结束 (0 + 248)
        int[] tx = {SRC_X, SRC_X + BORDER_SIZE, SRC_X + BORDER_SIZE + srcCenterWidth, SRC_X + SRC_WIDTH};

        // S: 屏幕 Y 坐标的四个关键点
        int[] sy = {y, y + BORDER_SIZE, y + BORDER_SIZE + targetCenterHeight, y + targetHeight};

        // T: 纹理 Y 坐标的四个关键点 (像素值)
        int[] ty = {SRC_Y, SRC_Y + BORDER_SIZE, SRC_Y + BORDER_SIZE + srcCenterHeight, SRC_Y + SRC_HEIGHT};

        // 循环绘制 9 个部分 (i 对应 X 轴，j 对应 Y 轴)
        for (int i = 0; i < 3; i++) { // 遍历 X (Left, Center, Right)
            for (int j = 0; j < 3; j++) { // 遍历 Y (Top, Middle, Bottom)

                // 1. 屏幕坐标 (X0, Y0, X1, Y1)
                int screenX0 = sx[i];
                int screenY0 = sy[j];
                int screenX1 = sx[i + 1];
                int screenY1 = sy[j + 1];

                // 2. 纹理坐标 (U0, V0, U1, V1 - 像素)
                int texU0 = tx[i];
                int texV0 = ty[j];
                int texU1 = tx[i + 1];
                int texV1 = ty[j + 1];

                // 如果屏幕上的绘制区域无效，则跳过
                if (screenX1 <= screenX0 || screenY1 <= screenY0) continue;

                // 3. 计算 UV 比例 (U/V 范围 [0.0, 1.0])
                float u0 = (float) texU0 / TEX_FULL_WIDTH;
                float v0 = (float) texV0 / TEX_FULL_HEIGHT;
                float u1 = (float) texU1 / TEX_FULL_WIDTH;
                float v1 = (float) texV1 / TEX_FULL_HEIGHT;

                // 4. 调用 blit 方法
                // blit(ResourceLocation atlas, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1)
                graphics.blit(
                        texture,
                        screenX0, screenY0, // x0, y0: 屏幕左上角
                        screenX1, screenY1, // x1, y1: 屏幕右下角
                        u0, u1,             // 纹理U比例
                        v0, v1              // 纹理V比例
                );
            }
        }
    }

}
