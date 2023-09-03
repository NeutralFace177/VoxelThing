package io.bluestaggo.voxelthing.gui;

import io.bluestaggo.voxelthing.Game;
import io.bluestaggo.voxelthing.gui.control.GuiControl;
import io.bluestaggo.voxelthing.gui.control.LabeledButton;
import io.bluestaggo.voxelthing.renderer.GLState;
import io.bluestaggo.voxelthing.renderer.MainRenderer;
import io.bluestaggo.voxelthing.renderer.draw.Quad;
import static org.lwjgl.glfw.GLFW.*;

public class EscMenu extends GuiScreen{

    public final GuiControl returnToMenu;

    public EscMenu(Game game) {
        super(game);
        returnToMenu = addControl(new LabeledButton(this)
			.withText("Title Screen")
			.at(-50.0f, 10.0f)
			.size(100.0f, 20.0f)
			.alignedAt(0.5f, 0.5f)
		);
    }

    public void draw() {
		MainRenderer r = game.renderer;

			

			r.fonts.outlined.printCentered("A B", r.screen.getWidth() / 2.0f, 10.0f);
		

		super.draw();
	}

    @Override
    public void onControlClicked(GuiControl control, int button) {
		if (control == returnToMenu) {
			game.openGui(new SaveSelect(game));
		}
		
	}

    @Override
    protected void onKeyPressed(int key) {
        if (key == GLFW_KEY_ESCAPE) {
			game.closeGui();
		}
    }
    
}
