package kraken.stub;

import static kraken.plugin.api.Actions.MENU_EXECUTE_NPC1;
import static kraken.plugin.api.Client.MINING;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import kraken.plugin.api.Actions;
import kraken.plugin.api.Bank;
import kraken.plugin.api.Cache;
import kraken.plugin.api.CacheItem;
import kraken.plugin.api.CacheNpc;
import kraken.plugin.api.CacheObject;
import kraken.plugin.api.Client;
import kraken.plugin.api.ConVar;
import kraken.plugin.api.Debug;
import kraken.plugin.api.Dialogue;
import kraken.plugin.api.Effect;
import kraken.plugin.api.Effects;
import kraken.plugin.api.Entity;
import kraken.plugin.api.EquipmentSlot;
import kraken.plugin.api.GroundItem;
import kraken.plugin.api.GroundItems;
import kraken.plugin.api.ImGui;
import kraken.plugin.api.Inventory;
import kraken.plugin.api.Item;
import kraken.plugin.api.Kraken;
import kraken.plugin.api.Npc;
import kraken.plugin.api.Npcs;
import kraken.plugin.api.Player;
import kraken.plugin.api.Players;
import kraken.plugin.api.Plugin;
import kraken.plugin.api.PluginContext;
import kraken.plugin.api.SceneObject;
import kraken.plugin.api.SceneObjects;
import kraken.plugin.api.Vector2i;
import kraken.plugin.api.Widget;
import kraken.plugin.api.WidgetGroup;
import kraken.plugin.api.WidgetItem;
import kraken.plugin.api.Widgets;

/**
 * An example plugin.
 */
public class ExamplePlugin extends Plugin {

    private boolean testWithdrawing = false;
    private boolean testDepositing = false;
    private boolean testNpcInteract = false;
    private boolean testBankInteract = false;
    private boolean testLogout = false;

    @Override
    public boolean onLoaded(PluginContext pluginContext) {
        pluginContext.setName("Example");
        return true;
    }

    @Override
    public int onLoop() {
        if (Bank.isOpen()) {
            if (testWithdrawing) {
                Bank.withdraw((item) -> item.getSlot() < 10, 1);
            }

            if (testDepositing) {
                Bank.deposit((item) -> true, 1);
            }
        }

        if (testNpcInteract) {
            Npc firstNpc = Npcs.closest((npc) -> npc.getName() != null && !npc.getName().isEmpty());
            if (firstNpc != null) {
                Actions.menu(MENU_EXECUTE_NPC1, firstNpc.getServerIndex(), 0, 0, 1);
            }
        }

        if (testBankInteract) {
            SceneObject bank = SceneObjects.closest((obj) -> obj.getName().equalsIgnoreCase("Bank booth"));
            if (bank != null) {
                bank.interact("Examine");
                bank.interact("Bank");
            }
        }

        if (testLogout) {
            Client.logout();
            testLogout = false;
        }

        return 8000;
    }

    /**
     * Prints all text in a widget recursively.
     */
    private void printWidgetText(Widget w) {
        int type = w.getType();
        if (type == Widget.TEXT) {
            String text = w.getText();
            if (text != null) {
                ImGui.label("   -> " + text);
            }
        } else if (type == Widget.CONTAINER) {
            Widget[] children = w.getChildren();
            if (children != null) {
                for (Widget child : children) {
                    if (child != null) {
                        printWidgetText(child);
                    }
                }
            }
        }
    }

    private void paintControls() {
        ImGui.checkbox("Checkbox", true);
        ImGui.label("Label");
        ImGui.intSlider("Int Slider", 37, 1, 100);
        ImGui.intInput("Int Input", 40);
        ImGui.button("Button");
    }

    private void paintCache() {
        CacheItem cacheItem = Cache.getItem(4151);
        ImGui.label("CacheItem= " + cacheItem.getId() + " " + cacheItem.getName() + " " + Long.toHexString(cacheItem.getAddress()));
        for (String option : cacheItem.getOptionNames()) {
            ImGui.label("  -> " + option);
        }

        CacheNpc cacheNpc = Cache.getNpc(19501);
        ImGui.label("CacheNpc= " + cacheNpc.getId() + " " + cacheNpc.getName() + " " + Long.toHexString(cacheNpc.getAddress()));
        for (String option : cacheNpc.getOptionNames()) {
            ImGui.label("  -> " + option);
        }

        CacheObject cacheObject = Cache.getObject(451);
        ImGui.label("CacheObject= " + cacheObject.getId() + " " + cacheObject.getName() + " " + Long.toHexString(cacheObject.getAddress()));
        for (String option : cacheObject.getOptionNames()) {
            ImGui.label("  -> " + option);
        }

        int[] mageIds = {
                2753, 2754, 2755
        };

        for (int mageId : mageIds) {
            CacheNpc mage = Cache.getNpc(mageId);
            ImGui.label("VarbitTest= " + mage.getId() + " " + mage.getName() + " " + Long.toHexString(mage.getAddress()));
            for (String option : mage.getOptionNames()) {
                ImGui.label("  -> " + option);
            }
        }
    }

    private void paintClient() {
        ImGui.label("State= " + Client.getState());
        ImGui.label("Loading= " + Client.isLoading());
        ImGui.label("ConVar= " + Client.getConVarById(3913));
        ImGui.label("Mining= " + Client.getStatById(MINING));
        ImGui.label("Restarts= " + Kraken.getRestartCount());
    }

    private void paintSelf() {
        Player self = Players.self();
        if (self != null) {
            ImGui.label("Self");
            ImGui.label(" -> Name= " + self.getName());
            ImGui.label(" -> Animation= " + self.getAnimationId());
            ImGui.label(" -> Moving= " + self.isMoving());
            ImGui.label(" -> GlobalPos= " + self.getGlobalPosition());
            ImGui.label(" -> ScenePos= " + self.getScenePosition());
            ImGui.label(" -> Status= " + self.getStatusBarFill());
            ImGui.label(" -> DirOff= " + self.getDirectionOffset());

            Entity interacting = self.getInteracting();
            if (interacting != null) {
                ImGui.label(" -> Interacting= " + interacting.getName());
            }

            Map<EquipmentSlot, Item> equipment = self.getEquipment();
            for (EquipmentSlot slot : equipment.keySet()) {
                if (equipment.get(slot).getId() != -1) {
                    ImGui.label(" -> Equipment= " + slot + " " + equipment.get(slot).getId() + " " + equipment.get(slot).getName());
                }
            }
        }
    }

    private void paintPlayers() {
        AtomicInteger playerForEachChecksum = new AtomicInteger();
        Players.forEach((plr) -> {
            playerForEachChecksum.incrementAndGet();
        });
        ImGui.label("Players#forEach= " + playerForEachChecksum);
        ImGui.label("Players#all= " + Players.all().length);
    }

    private void paintNpcs() {
        AtomicInteger npcForEachChecksum = new AtomicInteger();
        Npcs.forEach((npc) -> {
            npcForEachChecksum.incrementAndGet();
        });
        ImGui.label("Npcs#forEach= " + npcForEachChecksum);
        ImGui.label("Npcs#all= " + Npcs.all().length);

        Npc firstNpc = Npcs.closest((npc) -> npc.getName() != null && !npc.getName().isEmpty());
        if (firstNpc != null) {
            ImGui.label("Npcs#closest");
            ImGui.label(" -> Name= " + firstNpc.getName());
            ImGui.label(" -> Id= " + firstNpc.getId());
            ImGui.label(" -> Animation= " + firstNpc.getAnimationId());
            ImGui.label(" -> Moving= " + firstNpc.isMoving());
            ImGui.label(" -> Health= " + firstNpc.getHealth());
            ImGui.label(" -> GlobalPos= " + firstNpc.getGlobalPosition());
            ImGui.label(" -> ScenePos= " + firstNpc.getScenePosition());
            ImGui.label(" -> DirOff= " + firstNpc.getDirectionOffset());

            Entity interacting = firstNpc.getInteracting();
            if (interacting != null) {
                ImGui.label(" -> Interacting= " + interacting.getName());
            }
        }
    }

    private void paintObjects() {
        SceneObject firstObj = SceneObjects.closest((obj) -> obj.getName() != null && !obj.getName().isEmpty());
        if (firstObj != null) {
            ImGui.label("Obj");
            ImGui.label(" -> Name= " + firstObj.getName());
            ImGui.label(" -> Id= " + firstObj.getId());
            ImGui.label(" -> GlobalPos= " + firstObj.getGlobalPosition());
            ImGui.label(" -> ScenePos= " + firstObj.getScenePosition());
        }

        AtomicInteger objectForEachChecksum = new AtomicInteger();
        SceneObjects.forEach((plr) -> {
            objectForEachChecksum.incrementAndGet();
        });
        ImGui.label("SceneObjects#forEach= " + objectForEachChecksum);
    }

    private void paintGroundItems() {
        GroundItem firstGround = GroundItems.closest((obj) -> true);
        if (firstGround != null) {
            ImGui.label("GroundItem");
            ImGui.label(" -> Id= " + firstGround.getId());
            ImGui.label(" -> GlobalPos= " + firstGround.getGlobalPosition());
            ImGui.label(" -> ScenePos= " + firstGround.getScenePosition());
        }
    }

    private void paintEffects() {
        Effect effect = Effects.closest((obj) -> true);
        if (effect != null) {
            ImGui.label("Effect");
            ImGui.label(" -> Id= " + effect.getId());
            ImGui.label(" -> GlobalPos= " + effect.getGlobalPosition());
            ImGui.label(" -> ScenePos= " + effect.getScenePosition());
        }

        AtomicInteger effectForEachChecksum = new AtomicInteger();
        Effects.forEach((plr) -> {
            effectForEachChecksum.incrementAndGet();
        });
        ImGui.label("Effects#forEach= " + effectForEachChecksum);
        ImGui.label("Effects#all= " + Effects.all().length);
    }

    private void paintWidgets() {
        WidgetGroup bankWidget = Widgets.getGroupById(517);
        if (bankWidget != null) {
            ImGui.label("Bank");
            ImGui.label(" -> Group= " + bankWidget.getId());

            Widget[] widgets = bankWidget.getWidgets();
            for (Widget w : widgets) {
                ImGui.label("  -> Type= " + w.getType());
                ImGui.label("  -> Bounds= " + w.getPosition() + " " + w.getSize());
                printWidgetText(w);
            }
        }

        ImGui.label("Inventory");
        for (WidgetItem item : Inventory.getItems()) {
            ImGui.label(" -> " + item);
        }
    }

    private void paintDialogue() {
        ImGui.label("Dialogue");
        for (String s : Dialogue.getOptions()) {
            ImGui.label(" -> " + s);
        }
    }

    @Override
    public void onPaint() {
        if (ImGui.beginTabBar("TestTabs")) {
            if (ImGui.beginTabItem("Controls")) {
                testWithdrawing = ImGui.checkbox("Test Withdrawing", testWithdrawing);
                testDepositing = ImGui.checkbox("Test Depositing", testDepositing);
                testNpcInteract = ImGui.checkbox("Test NPC Interact", testNpcInteract);
                testBankInteract = ImGui.checkbox("Test Bank Interact", testBankInteract);
                testLogout = ImGui.checkbox("Test Logout", testLogout);

                paintControls();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Cache")) {
                paintCache();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Client")) {
                paintClient();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Self")) {
                paintSelf();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Players")) {
                paintPlayers();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("NPCs")) {
                paintNpcs();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Objects")) {
                paintObjects();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Ground Items")) {
                paintGroundItems();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Effects")) {
                paintEffects();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Widgets")) {
                paintWidgets();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Dialogue")) {
                paintDialogue();
                ImGui.endTabItem();
            }

            ImGui.endTabBar();
        }
    }

    @Override
    public void onPaintOverlay() {
        ImGui.freeText("Free Text", new Vector2i(15, 15), 0xff0000ff);
        ImGui.freeLine(new Vector2i(15, 15), new Vector2i(45, 45), 0xff0000ff);
        ImGui.freePoly4(new Vector2i(150, 150), new Vector2i(158, 167), new Vector2i(132, 154), new Vector2i(128, 128), 0xff0000ff);

        Players.forEach((p) -> {
            Vector2i mm = Client.worldToMinimap(p.getScenePosition());
            if (mm != null) {
                ImGui.freeText(p.getName(), mm, 0xff0000ff);
            }
        });
    }

    @Override
    public void onConVarChanged(ConVar conv, int oldValue, int newValue) {
        Debug.log("Connection variable changed! " + oldValue + " " + newValue);
    }

    @Override
    public void onWidgetVisibilityChanged(int id, boolean visible) {
        Debug.log("Widget visibility changed! " + id + " " + visible);
    }

    @Override
    public void onInventoryItemChanged(WidgetItem prev, WidgetItem next) {
        Debug.log("Inventory item changed! " + prev + " " + next);
    }
}
