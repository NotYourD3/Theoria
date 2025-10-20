package pers.notyourd3.theoria.client.gui;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.*;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import pers.notyourd3.theoria.client.ClientTheoryManager;
import pers.notyourd3.theoria.theory.Theory;
import pers.notyourd3.theoria.theory.TheoryCategory;

import java.util.List;
import java.util.Objects;

public class TheoriaScreen extends Screen {
    private static final int TILE_SIZE = 16;

    private final ClientTheoryManager manager;
    private List<TheoryCategory> categories;
    @Nullable
    private TheoryCategory selectedCategory;
    @Nullable
    private Theory hoveredTheory;

    // 用于平移理论树视图
    private double panX = 0;
    private double panY = 0;
    private boolean isDragging = false;

    // 左侧分类列表的宽度
    private static final int CATEGORY_LIST_WIDTH = 120;
    private static final int CATEGORY_ENTRY_HEIGHT = 24;
    private double dragStartX;
    private double dragStartY;

    public TheoriaScreen() {
        super(Component.translatable("gui.theoria.title"));
        this.manager = ClientTheoryManager.getInstance();
    }

    @Override
    protected void init() {
        super.init();
        this.categories = manager.getCategoriesById().values().stream().toList();
        if (this.selectedCategory == null && !this.categories.isEmpty()) {
            this.selectedCategory = this.categories.get(0);
        }
        // 重置平移状态
        this.panX = (this.width - CATEGORY_LIST_WIDTH) / 2.0;
        this.panY = this.height / 2.0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //super.render(graphics, mouseX, mouseY, partialTick);
        this.hoveredTheory = null; // 每帧重置

        // 1. 渲染背景
        // this.renderBackground(graphics, mouseX, mouseY, partialTick); // 默认的半透明背景

        // 2. 渲染理论树区域 (在分类列表右侧)
        if (this.selectedCategory != null) {
            // 启用剪裁区域，防止理论树渲染到分类列表上
            graphics.enableScissor(CATEGORY_LIST_WIDTH, 0, this.width, this.height);

            renderTheoryTreeBackground(graphics);
            renderDependencies(graphics, mouseX, mouseY);
            renderTheories(graphics, mouseX, mouseY);

            graphics.disableScissor();
        }

        // 3. 渲染左侧的分类列表 (覆盖在最上层)
        renderCategoryList(graphics, mouseX, mouseY);

        // 4. 渲染工具提示
        if (this.hoveredTheory != null) {
            List<ClientTooltipComponent> tooltipLines = List.of(
                    new ClientTextTooltip(this.hoveredTheory.title().getVisualOrderText()),
                    new ClientTextTooltip(this.hoveredTheory.subTitle().getVisualOrderText())
            );
            graphics.renderTooltip(this.font, tooltipLines, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE,null);
        }
    }

    private void renderTheoryTreeBackground(GuiGraphics graphics) {
        if (selectedCategory == null) return;
        final int TILE_SIZE = 16;
        final float UV_START = 0.0F;
        final float UV_END = 1.0F;
        int areaXStart = CATEGORY_LIST_WIDTH;
        int areaYStart = 0;
        int areaXEnd = this.width;
        int areaYEnd = this.height;
        ResourceLocation backgroundTexture = selectedCategory.background();
        int offsetX = (int) (panX % TILE_SIZE);
        int offsetY = (int) (panY % TILE_SIZE);
        if (offsetX > 0) offsetX -= TILE_SIZE;
        if (offsetY > 0) offsetY -= TILE_SIZE;
        for (int y = areaYStart + offsetY; y < areaYEnd; y += TILE_SIZE) {
            for (int x = areaXStart + offsetX; x < areaXEnd; x += TILE_SIZE) {
                int screenX1 = Math.min(x + TILE_SIZE, areaXEnd);
                int screenY1 = Math.min(y + TILE_SIZE, areaYEnd);
                if (screenX1 <= x || screenY1 <= y) continue;
                int drawWidth = screenX1 - x;
                int drawHeight = screenY1 - y;

                float u1 = (float) drawWidth / TILE_SIZE * UV_END;
                float v1 = (float) drawHeight / TILE_SIZE * UV_END;
                graphics.blit(backgroundTexture, x, y, screenX1, screenY1, UV_START, u1, UV_START, v1);
            }
        }
    }


    private void renderDependencies(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Theory> theories = manager.getTheoriesByCategory().get(selectedCategory);
        if (theories == null) return;

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) panX, (float) panY);

        for (Theory theory : theories) {
            List<Theory> dependencies = manager.getDependencies(theory);
            for (Theory dep : dependencies) {
                // 确保依赖项和理论都在当前分类中
                if (Objects.equals(dep.categoryId(), selectedCategory.id())) {
                    drawDependencyLine(graphics, dep.x(), dep.y(), theory.x(), theory.y());
                }
            }
        }
        graphics.pose().popMatrix();
    }

    private void renderTheories(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Theory> theories = manager.getTheoriesByCategory().get(selectedCategory);
        if (theories == null) return;

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) panX, (float) panY);

        // 将屏幕鼠标坐标转换为理论树内的相对坐标
        double relativeMouseX = mouseX - panX;
        double relativeMouseY = mouseY - panY;

        for (Theory theory : theories) {
            int iconXp = theory.x() - 13;
            int iconYp = theory.y() - 13;
            int iconSizep = 26;
            int iconX = theory.x() - 8; // 图标中心对齐
            int iconY = theory.y() - 8;
            int iconSize = 16;

            //boolean isUnlocked = manager.isTheoryUnlocked(theory.id());

            // 设置颜色，如果未解锁则变灰
            //int tint = isUnlocked ? 0xFFFFFFFF : 0xFF808080;
            String namespace = ClientTheoryManager.getInstance().canTheoryGrant(theory) ? "minecraft" : "theoria";
            String un = ClientTheoryManager.getInstance().isTheoryUnlocked(theory.id()) ? "" : "un";
            graphics.blit(ResourceLocation.fromNamespaceAndPath(namespace,"textures/gui/sprites/advancements/" + theory.type() + "_frame_"+ un +"obtained.png"), iconXp,iconYp,iconXp+26,iconYp+26, 0, 1, 0, 1);
            if(theory.icon().getPath().contains(".png")){
            graphics.blit(theory.icon(),
                    iconX, iconY, iconX+16,iconY+16, 0, 1, 0, 1);
            }else{
                graphics.renderItem(Item.byId(BuiltInRegistries.ITEM.getId(theory.icon())).getDefaultInstance(), iconX, iconY);
            }

            // 检查鼠标是否悬停
            if (relativeMouseX >= iconXp && relativeMouseX < iconXp + iconSizep &&
                    relativeMouseY >= iconYp && relativeMouseY < iconYp + iconSizep) {
                this.hoveredTheory = theory;
                int outlineColor = 0xFFFFFFFF;
                int thickness = 1;
                graphics.fill(iconXp, iconYp, iconXp + iconSizep, iconYp + thickness, outlineColor);
                // 底部边框
                graphics.fill(iconXp, iconYp + iconSizep - thickness, iconXp + iconSizep, iconYp + iconSizep, outlineColor);
                // 左侧边框
                graphics.fill(iconXp, iconYp + thickness, iconXp + thickness, iconYp + iconSizep - thickness, outlineColor);
                // 右侧边框
                graphics.fill(iconXp + iconSizep - thickness, iconYp + thickness, iconXp + iconSizep, iconYp + iconSizep - thickness, outlineColor);
            }
        }
        graphics.pose().popMatrix();
    }

    private void renderCategoryList(GuiGraphics graphics, int mouseX, int mouseY) {
        // 绘制背景
        //graphics.fill(0, 0, CATEGORY_LIST_WIDTH, this.height, 0x80000000); // 半透明黑色背景
        TheoryDescriptionScreen.renderNineSlice(graphics,ResourceLocation.withDefaultNamespace("textures/gui/demo_background.png"), 0, 0, CATEGORY_LIST_WIDTH, this.height);

        int yOffset = 5;
        for (TheoryCategory category : this.categories) {
            // 如果是选中的分类，绘制一个高亮背景
            if (category.equals(this.selectedCategory)) {
                graphics.fill(5, yOffset - 2, CATEGORY_LIST_WIDTH - 5, yOffset + CATEGORY_ENTRY_HEIGHT - 2, 0x40FFFFFF);
            }

            //graphics.blit(category.icon(), 10, yOffset + 4, 0, 0, 16, 16, 16, 16);
            graphics.drawString(this.font, category.title(), 16, yOffset + 8, 0xFFFFFFFF);
            if(category.icon().getPath().contains(".png")){
                graphics.blit(category.icon(),
                        8, yOffset, 24,16+yOffset, 0, 1, 0, 1);
            }else{
                graphics.renderItem(Item.byId(BuiltInRegistries.ITEM.getId(category.icon())).getDefaultInstance(), 0, yOffset);
            }

            yOffset += CATEGORY_ENTRY_HEIGHT;
        }
    }

    private void drawDependencyLine(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        // 这是一个简化的划线实现。更复杂的箭头需要自定义顶点渲染。
        // `screens.md` 提到了 `GuiElementRenderState`，但对于简单的线，我们可以用 `fill`。
        float angle = (float) Math.atan2(y2 - y1, x2 - x1);
        float length = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));

        graphics.pose().pushMatrix();
        graphics.pose().translate(x1, y1);
        graphics.pose().rotate(angle);
        graphics.fill(0, -1, (int)length, 1, 0xFFFFFFFF); // 画一条1像素宽的白线
        graphics.pose().popMatrix();
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent event,boolean isdoubleClick) {
        if (event.button() == 0) { // 左键点击
            if (event.x() < CATEGORY_LIST_WIDTH) {
                int yOffset = 5;
                for (TheoryCategory category : this.categories) {
                    if (event.y() >= yOffset && event.y() < yOffset + CATEGORY_ENTRY_HEIGHT) {
                        this.selectedCategory = category;
                        this.panX = (this.width - CATEGORY_LIST_WIDTH) / 2.0;
                        this.panY = this.height / 2.0;
                        return true;
                    }
                    yOffset += CATEGORY_ENTRY_HEIGHT;
                }
            } else {
                if (this.hoveredTheory != null && isdoubleClick) {
                    Minecraft.getInstance().setScreen(new TheoryDescriptionScreen(this.hoveredTheory, this));
                    return true;
                }

                // 开始拖动
                this.isDragging = true;
                this.dragStartX = event.x() - this.panX;
                this.dragStartY = event.y() - this.panY;
                return true;
            }
        }
        return super.mouseClicked(event,isdoubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event,double mouseX,double mouseY) {
        if (this.isDragging && event.button() == 0) {
            this.panX = event.x() - this.dragStartX;
            this.panY = event.y() - this.dragStartY;
            return true;
        }
        return super.mouseDragged(event, mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            this.isDragging = false;
        }
        return super.mouseReleased(event);
    }



    @Override
    public boolean isPauseScreen() {
        // 返回 false 意味着在打开此GUI时游戏不会暂停
        return false;
    }
}