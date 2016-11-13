package vswe.stevesvehicles.arcade.tracks;

import java.util.ArrayList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import vswe.stevesvehicles.StevesVehicles;
import vswe.stevesvehicles.arcade.ArcadeGame;
import vswe.stevesvehicles.client.ResourceHelper;
import vswe.stevesvehicles.client.gui.screen.GuiVehicle;
import vswe.stevesvehicles.localization.entry.arcade.LocalizationTrack;
import vswe.stevesvehicles.module.common.attachment.ModuleArcade;
import vswe.stevesvehicles.network.DataReader;
import vswe.stevesvehicles.network.DataWriter;
import vswe.stevesvehicles.vehicle.VehicleBase;

public class ArcadeTracks extends ArcadeGame {

	public ArcadeTracks(ModuleArcade module) {
		super(module, LocalizationTrack.TITLE);

		carts = new ArrayList<>();

		carts.add(player = new Cart(0) {
			@Override
			public void onItemPickUp() {
				completeLevel();	
				playSound("win", 1, 1);
			}

			@Override
			public void onCrash() {				
				if (isPlayingFinalLevel() && currentStory < unlockedLevels.length - 1 && unlockedLevels[currentStory + 1] == -1) {
					sendPacket(currentStory + 1, 0);
				}
			}
		});


		carts.add(enderman = new Cart(1));
		lists = new ArrayList<>();
		lists.add(storyList = new ScrollableList(this, 5, 40) {
			@Override
			public boolean isVisible() {
				return currentMenuTab == 0 && !storySelected;
			}					
		});


		lists.add(mapList = new ScrollableList(this, 5, 40) {
			@Override
			public boolean isVisible() {
				return currentMenuTab == 0 && storySelected;
			}					
		});	

		lists.add(userList = new ScrollableList(this, 5, 40) {
			@Override
			public boolean isVisible() {
				return currentMenuTab == 1;
			}					
		});			


		unlockedLevels = new int[TrackStory.stories.size()];
		unlockedLevels[0] = 0;
		for (int i = 1; i < unlockedLevels.length; i++) {
			unlockedLevels[i] = -1;
		}

		loadStories();
		if (getModule().getVehicle().getWorld().isRemote) {
			loadUserMaps();
		}
	}

	private void loadStories() {
		storyList.clearList();

		for (int i = 0; i < TrackStory.stories.size(); i++) {
			if (unlockedLevels[i] > -1) {
				storyList.add(TrackStory.stories.get(i).getName());
			}else{
				storyList.add(null);
			}
		}
	}

	private void loadMaps() {
		int story = storyList.getSelectedIndex();
		if (story != -1) {
			ArrayList<TrackLevel> levels = TrackStory.stories.get(story).getLevels();
			mapList.clearList();
			for(int i = 0; i < levels.size(); i++) {
				if (unlockedLevels[story] >= i) {
					mapList.add(levels.get(i).getName());	
				}else{
					mapList.add(null);
				}
			}	
		}
	}


	@SideOnly(Side.CLIENT)
	private void loadUserMaps() {	
		userList.clearList();

		userMaps = TrackLevel.loadMapsFromFolder();

		if (StevesVehicles.arcadeDevOperator) {
			for (int i = 0; i < TrackStory.stories.size(); i++) {
				for (int j = 0; j < TrackStory.stories.get(i).getLevels().size(); j++) {
					userMaps.add(TrackStory.stories.get(i).getLevels().get(j));
				}
			}	
		}

		for (TrackLevel userMap : userMaps) {
			userList.add(userMap.getName());
		}
	}

	private void loadMap(int story, int level) {
		currentStory = story;
		currentLevel = level;		
		loadMap(TrackStory.stories.get(story).getLevels().get(level));
	}

	private void loadMap(TrackLevel map) {
		isUsingEditor = false;
		trackMap = new Track[27][10];
		tracks = new ArrayList<>();		

		for (Track track : map.getTracks()) {
			Track newTrack = track.copy();
			tracks.add(newTrack);
			if (newTrack.getX() >= 0 && newTrack.getX() < trackMap.length && newTrack.getY() >= 0 && newTrack.getY() < trackMap[0].length) {
				trackMap[newTrack.getX()][newTrack.getY()] = newTrack;
			}
		}	

		hoveringTrack = null;
		editorTrack = null;
		editorDetectorTrack = null;
		currentMap = map;
		isRunning = false;
		playerStartX = currentMap.getPlayerStartX();
		playerStartY = currentMap.getPlayerStartY();
		playerStartDirection = currentMap.getPlayerStartDirection();
		itemX = currentMap.getItemX();
		itemY = currentMap.getItemY();		
		resetPosition();		
	}


	private void resetPosition() {
		tick = 0;
		player.setX(playerStartX);
		player.setY(playerStartY);	
		isItemTaken = false;
		player.setDirection(TrackOrientation.Direction.STILL);
		enderman.setAlive(false);
	}


	private TrackLevel currentMap;

	private boolean isMenuOpen = true;
	private boolean isRunning = false;

	private int currentStory = -1;
	private int currentLevel = -1;
	private int[] unlockedLevels;

	ArrayList<Cart> carts;
	private Cart player;
	private Cart enderman;
	private int playerStartX;
	private int playerStartY;
	private TrackOrientation.Direction playerStartDirection;

	private int itemX;
	private int itemY;
	private boolean isItemTaken;

	private ArrayList<Track> tracks;
	private Track[][] trackMap;


	private int tick;

	private int currentMenuTab = 0;
	private ArrayList<ScrollableList> lists;
	private boolean storySelected;
	private ScrollableList storyList;
	private ScrollableList mapList;
	private ScrollableList userList;
	private ArrayList<TrackLevel> userMaps;
	private boolean isUsingEditor;
	private boolean isSaveMenuOpen;
	private boolean failedToSave;
	private String saveName = "";
	private String lastSavedName = "";



	public Track[][] getTrackMap() {
		return trackMap;
	}


	public Cart getEnderman() {
		return enderman;
	}

	private boolean isPlayingFinalLevel() {
		return isPlayingNormalLevel() && currentLevel == TrackStory.stories.get(currentStory).getLevels().size() - 1;
	}

	private boolean isUsingEditor() {
		return isUsingEditor;
	}	

	private boolean isPlayingUserLevel() {
		return currentStory == -1;
	}

	private boolean isPlayingNormalLevel() {
		return !isUsingEditor() && !isPlayingUserLevel();
	}

	@Override
	public void update() {
		super.update();
		if (isRunning) {
			if (tick == 3) {	
				for (Cart cart : carts) {
					cart.move(this);
				}

				tick = 0;
			}else{
				tick++;
			}
		}
	}


	@Override
	public void drawForeground(GuiVehicle gui) {
		if (isSaveMenuOpen) {
			int[] menu = getSaveMenuArea();
			if (failedToSave) {
				getModule().drawString(gui, LocalizationTrack.SAVE_ERROR.translate(), menu[0] + 3, menu[1] + 3, 0xFF0000);
			}else{
				getModule().drawString(gui, LocalizationTrack.SAVE_MESSAGE.translate(), menu[0] + 3, menu[1] + 3, 0x404040);
			}
			getModule().drawString(gui, saveName + (saveName.length() < 15 && getModule().getVehicle().getWorld().getWorldTime() % 20 < 10 ? "|" : ""), menu[0] + 5, menu[1] + 16, 0xFFFFFF);

		}else if (isMenuOpen) {
			for (ScrollableList list : lists) {
				list.drawForeground(gui);
			}

			if (currentMenuTab == 0 || currentMenuTab == 1) {
				int[] menu = getMenuArea();

				String str;
				if (currentMenuTab == 1) {
					str = LocalizationTrack.USER_MAPS.translate();
				}else if (storySelected) {
					str = TrackStory.stories.get(storyList.getSelectedIndex()).getName();
				}else{
					str = LocalizationTrack.STORIES.translate();
				}
				getModule().drawString(gui, str, menu[0] + 5, menu[1] + 32, 0x404040);
			}else{
				int[] menu = getMenuArea();

				getModule().drawSplitString(gui, LocalizationTrack.HELP.translate(), menu[0] + 10, menu[1] + 20, menu[2] - 20, 0x404040);
			}
		}else{
			for(LevelMessage message : currentMap.getMessages()) {
				if (message.isVisible(isRunning, isRunning && player.getDirection() == TrackOrientation.Direction.STILL, isRunning && isItemTaken)) {
					getModule().drawSplitString(gui, message.getMessage(), LEFT_MARGIN + 4 + message.getX() * 16, TOP_MARGIN + 4 + message.getY() * 16, message.getW() * 16, 0x404040);
				}
			}

			if (isUsingEditor()) {
				getModule().drawString(gui, "1-5 - " + LocalizationTrack.TRACK_SHAPE.translate(), 10, 180, 0x404040);
				getModule().drawString(gui, "R - " + LocalizationTrack.TRACK_ROTATE.translate(), 10, 190, 0x404040);
				getModule().drawString(gui, "F - " + LocalizationTrack.TRACK_FLIP.translate(), 10, 200, 0x404040);
				getModule().drawString(gui, "A - " + LocalizationTrack.TRACK_DIRECTION.translate(), 10, 210, 0x404040);
				getModule().drawString(gui, "T - " + LocalizationTrack.TRACK_TYPE.translate(), 10, 220, 0x404040);
				getModule().drawString(gui, "D - " + LocalizationTrack.TRACK_DELETE.translate(), 10, 230, 0x404040);
				getModule().drawString(gui, "C - " + LocalizationTrack.TRACK_COPY.translate(), 10, 240, 0x404040);

				getModule().drawString(gui, "S - " + LocalizationTrack.MOVE_STEVE.translate(), 330, 180, 0x404040);
				getModule().drawString(gui, "X - " + LocalizationTrack.MOVE_MAP.translate(), 330, 190, 0x404040);
				getModule().drawString(gui, LocalizationTrack.LEFT_MOUSE.translate() + " - " + LocalizationTrack.PLACE_TRACK.translate(), 330, 200, 0x404040);
				getModule().drawString(gui, LocalizationTrack.RIGHT_MOUSE.translate() + " - " + LocalizationTrack.DESELECT_TRACK.translate(), 330, 210, 0x404040);
			}
		}
	}	

	public static final int LEFT_MARGIN = 5;	
	public static final int TOP_MARGIN = 5;

	private static final ResourceLocation TEXTURE_MENU = ResourceHelper.getResource("/gui/trackgamemenu.png");
	private static final ResourceLocation TEXTURE_GAME = ResourceHelper.getResource("/gui/trackgame.png");

	@Override
	public void drawBackground(GuiVehicle gui, int x, int y) {
		if (!isSaveMenuOpen && isMenuOpen) {
			ResourceHelper.bindResource(TEXTURE_MENU);

			getModule().drawImage(gui, getMenuArea(), 0, 0);

			for (int i = 0; i < 3; i++) {
				int [] rect = getMenuTabArea(i);

				boolean active = getModule().inRect(x, y, rect);
				boolean hidden = !active && i == currentMenuTab;

				if (!hidden) {
					getModule().drawImage(gui, rect[0], rect[1] + rect[3], 0, active ? 114 : 113, rect[2], 1);
				}
			}

			for (ScrollableList list : lists) {
				list.drawBackground(gui, x, y);
			}			

		}else if (currentMap != null) {			
			ResourceHelper.bindResource(TEXTURE_GAME);


			if (isUsingEditor() && !isRunning) {
				for (int i = 0; i < trackMap.length; i++) {
					for (int j = 0; j < trackMap[0].length; j++) {
						getModule().drawImage(gui, LEFT_MARGIN + i * 16, TOP_MARGIN + j * 16, 16, 128, 16, 16);
					}					
				}
			}

			for (Track track : tracks) {
				getModule().drawImage(gui, getTrackArea(track.getX(), track.getY()), 16 * track.getU(), 16 * track.getV(), track.getRotation());
			}

			if (isUsingEditor()) {
				if (editorDetectorTrack != null && !isRunning) {
					editorDetectorTrack.drawOverlay(getModule(), gui, editorDetectorTrack.getX() * 16 + 8, editorDetectorTrack.getY() * 16 + 8, isRunning);
					getModule().drawImage(gui, LEFT_MARGIN + editorDetectorTrack.getX() * 16, TOP_MARGIN + editorDetectorTrack.getY() * 16, 32, 128, 16, 16);
				}
			}else{
				for (Track track : tracks) {
					track.drawOverlay(getModule(), gui, x, y, isRunning);
				}	
			}

			if (!isItemTaken) {
				int itemIndex = 0;
				if (isPlayingFinalLevel()) {
					itemIndex = 1;
				}
				getModule().drawImage(gui, LEFT_MARGIN + itemX * 16, TOP_MARGIN + itemY * 16, 16 * itemIndex, 256 - 16, 16, 16);
			}

			for (Cart cart : carts) {
				cart.render(this, gui, tick);
			}

			if (isUsingEditor() && !isRunning) {
				getModule().drawImage(gui, LEFT_MARGIN + playerStartX * 16, TOP_MARGIN + playerStartY * 16, 162, 212, 8, 8, playerStartDirection.getRenderRotation());
			}

			if (!isMenuOpen && editorTrack != null) {
				getModule().drawImage(gui, x - 8, y - 8, 16 * editorTrack.getU(), 16 * editorTrack.getV(), 16, 16, editorTrack.getRotation());
			}


			if (isSaveMenuOpen) {
				int[] rect = getSaveMenuArea();

				getModule().drawImage(gui, rect, 0, 144);
			}

		}



		ResourceHelper.bindResource(TEXTURE_GAME);
		for (int i = 0; i < BUTTON_COUNT; i++) {
			if (isButtonVisible(i)) {
				int[] rect = getButtonArea(i);
				int srcX = isButtonDisabled(i) ? 256 - 3*16 : getModule().inRect(x, y, rect) ? 256 - 2*16 : 256 - 16;
				int srcY = i*16;


				getModule().drawImage(gui, rect, srcX, srcY);
			}
		}		
	}

	@Override
	public void drawMouseOver(GuiVehicle gui, int x, int y) {
		for (int i = 0; i < BUTTON_COUNT; i++) {
			if (!isButtonDisabled(i) && isButtonVisible(i)) {
				getModule().drawStringOnMouseOver(gui, getButtonText(i), x, y, getButtonArea(i));
			}
		}			
	}	

	@Override
	public void mouseMovedOrUp(GuiVehicle gui,int x, int y, int button) {
		if (isSaveMenuOpen) {
			return;
		}

		if (isMenuOpen) {
			for (ScrollableList list : lists) {
				list.mouseMovedOrUp(gui, x, y, button);
			}			
		}


		if (currentMap != null && isUsingEditor()){
			int x2 = x - LEFT_MARGIN;
			int y2 = y - TOP_MARGIN;

			int gridX = x2 / 16;
			int gridY = y2 / 16;

			if (gridX >= 0 && gridX < trackMap.length && gridY >= 0 && gridY < trackMap[0].length) {
				hoveringTrack = trackMap[gridX][gridY];
			}else{
				hoveringTrack = null;
			}

		}

		handleEditorTrack(x, y, button, false);
	}		


	@Override
	public void mouseClicked(GuiVehicle gui, int x, int y, int button) {
		if (!isSaveMenuOpen) {

			if (isMenuOpen) {
				if (!getModule().inRect(x, y, getMenuArea())) {
					if (currentMap != null) {
						isMenuOpen = false;
					}
				}else{


					for (int i = 0; i < 3; i++) {
						if (i != currentMenuTab && getModule().inRect(x, y, getMenuTabArea(i))) {
							currentMenuTab = i;
							break;
						}
					}

					for (ScrollableList list : lists) {
						list.mouseClicked(gui, x, y, button);
					}

				}


			}else{

				if (!isRunning) {
					for (Track track : tracks) {
						if (getModule().inRect(x, y, getTrackArea(track.getX(), track.getY()))) {
							if (isUsingEditor()) {
								if (editorTrack == null) {
									track.onEditorClick(this);
								}
							}else{
								track.onClick(this);
							}
						}
					}

				}

				handleEditorTrack(x, y, button, true);
			}
		}

		for (int i = 0; i < BUTTON_COUNT; i++) {
			int[] rect = getButtonArea(i);

			if (getModule().inRect(x, y, rect)) {
				if (isButtonVisible(i) && !isButtonDisabled(i)) {
					buttonClicked(i);
					break;
				}		
			}
		}		

	}


	public void completeLevel() {
		if (isPlayingNormalLevel()) {
			int nextLevel = currentLevel + 1;
			if (nextLevel > unlockedLevels[currentStory]) {
				sendPacket(currentStory, nextLevel);
			}		
		}
	}

	private void sendPacket(int story, int level) {
		DataWriter dw = getDataWriter();
		dw.writeByte(story);
		dw.writeByte(level);
		sendPacketToServer(dw);
	}

	public int[] getMenuArea() {
		return new int[] {(VehicleBase.MODULAR_SPACE_WIDTH - 256) / 2, (VehicleBase.MODULAR_SPACE_HEIGHT - 113) / 2 ,256, 113};
	}

	private int[] getMenuTabArea(int id) {
		int [] menu = getMenuArea();

		return new int[] {menu[0] + 1 + id * 85, menu[1] + 1, 84, 12};
	}

	private int[] getSaveMenuArea() {
		return new int[] {(VehicleBase.MODULAR_SPACE_WIDTH - 99) / 2, (VehicleBase.MODULAR_SPACE_HEIGHT - 47) / 2 ,99, 47};
	}



	private final int BUTTON_COUNT = 14;

	private int[] getButtonArea(int id) {
		if (id == 4 || id == 5) {
			int [] menu = getMenuArea();
			return new int[] {menu[0] + 235 - 18 * (id - 4), menu[1] + 20, 16, 16};	
		}else if(id > 5 && id < 10) {
			int [] menu = getMenuArea();
			return new int[] {menu[0] + 235 , menu[1] + 20 + (id - 6) * 18, 16, 16};
		}else if(id >= 12 && id < 14) {
			int[] menu = getSaveMenuArea();
			return new int[] {menu[0] + menu[2] - 18 * (id - 11) - 2 , menu[1] + menu[3] - 18, 16, 16};
		}else{
			if (id >= 10 && id < 12) {
				id -= 6;
			}

			return new int[] {455, 26 + id * 18, 16, 16};
		}
	}	





	@SuppressWarnings("SimplifiableIfStatement") //easier to see this way
	private boolean isButtonVisible(int id) {
		if (id == 4 || id == 5) {
			return isMenuOpen && currentMenuTab == 0;
		}else if(id > 5 && id < 10) {
			return isMenuOpen && currentMenuTab == 1;
		}else if(id >= 10 && id < 12) {
			return isUsingEditor();
		}else if(id >= 12 && id < 14) {
			return isSaveMenuOpen;
		}else{
			return true;
		}
	}

	private boolean isButtonDisabled(int id) {
		switch (id) {
			case 0:
				return isRunning || isMenuOpen || isSaveMenuOpen;
			case 1:
				return isRunning || isMenuOpen || isSaveMenuOpen;
			case 2:
				return !isRunning || isSaveMenuOpen;	
			case 3:
				return isMenuOpen || isSaveMenuOpen || !isPlayingNormalLevel() || currentLevel + 1 > unlockedLevels[currentStory];
			case 4:
				return (storySelected ? mapList : storyList).getSelectedIndex() == -1;
			case 5:
				return !storySelected;
			case 6:
			case 8:
				return userList.getSelectedIndex() == -1;
			case 7:
			case 9:
			case 12:
				return false;
			case 10:
			case 11:
				return isMenuOpen || isSaveMenuOpen || isRunning;
			case 13:
				return saveName.length() == 0;
			default:
				return true;
		}
	}

	private void buttonClicked(int id) {
		switch (id) {
			case 0:
				for (Track track : tracks) {
					track.saveBackup();
				}
				player.setDirection(playerStartDirection);
				isRunning = true;
				break;
			case 1:
				isMenuOpen = true;
				editorTrack = null;
				break;
			case 2:
				for (Track track : tracks) {
					track.loadBackup();
				}				
				resetPosition();
				isRunning = false;
				break;
			case 3:
				loadMap(currentStory, currentLevel + 1);
				break;
			case 4:
				if (storySelected) {
					loadMap(storyList.getSelectedIndex(), mapList.getSelectedIndex());
					isMenuOpen = false;	
				}else{
					storySelected = true;					
					mapList.clear();
					loadMaps();
				}
				break;
			case 5:
				storySelected = false;
				break;
			case 6:		
				currentStory = -1;
				loadMap(userMaps.get(userList.getSelectedIndex()));			
				isMenuOpen = false;	
				break;
			case 7:
				loadMap(TrackLevel.editor);
				isMenuOpen = false;	
				lastSavedName = "";
				isUsingEditor = true;
				break;
			case 8:
				TrackLevel mapToEdit = userMaps.get(userList.getSelectedIndex());
				loadMap(mapToEdit);		
				lastSavedName = mapToEdit.getName();
				isMenuOpen = false;		
				isUsingEditor = true;
				break;
			case 9:
				userList.clear();
				if (getModule().getVehicle().getWorld().isRemote) {
					loadUserMaps();
				}
				break;
			case 10:
				if (lastSavedName.length() == 0) {
					isSaveMenuOpen = true;
					failedToSave = false;
				}else {
					save(lastSavedName);
				}
				break;
			case 11:
				isSaveMenuOpen = true;
				failedToSave = false;
				break;
			case 13:
				if (save(saveName)) {
					saveName = "";
					isSaveMenuOpen = false;
				}
				break;
			case 12:
				isSaveMenuOpen = false;
				break;
		}
	}

	private String getButtonText(int id) {
		switch (id) {
			case 0:
				return LocalizationTrack.START.translate();
			case 1:
				return LocalizationTrack.MENU.translate();
			case 2:
				return LocalizationTrack.STOP.translate();
			case 3:
				return LocalizationTrack.NEXT_LEVEL.translate();
			case 4:
				return storySelected ? LocalizationTrack.START_LEVEL.translate() : LocalizationTrack.SELECT_STORY.translate();
			case 5:
				return LocalizationTrack.SELECT_OTHER_STORY.translate();
			case 6:
				return LocalizationTrack.START_LEVEL.translate();
			case 7:
				return LocalizationTrack.CREATE_LEVEL.translate();
			case 8:
				return LocalizationTrack.EDIT_LEVEL.translate();
			case 9:
				return LocalizationTrack.REFRESH_LIST.translate();
			case 10:
				return LocalizationTrack.START.translate();
			case 11:
				return LocalizationTrack.SAVE_AS.translate();
			case 12:
				return LocalizationTrack.CANCEL.translate();
			case 13:
				return LocalizationTrack.SAVE.translate();
			default:
				return "Hello, I'm a button";
		}

	}

	public static int[] getTrackArea(int x, int y) {
		return new int[] {TOP_MARGIN + 16 * x, TOP_MARGIN + 16 * y, 16, 16};
	}

	public boolean isItemOnGround() {
		return !isItemTaken;
	}

	public void pickItemUp() {
		isItemTaken = true;
	}

	public int getItemX() {
		return itemX;
	}

	public int getItemY() {
		return itemY;
	}	

	@Override
	public void save(NBTTagCompound tagCompound) {
		for (int i = 0; i < unlockedLevels.length; i++) {
			tagCompound.setByte("Unlocked" + i, (byte)unlockedLevels[i]);
		}
	}

	@Override
	public void load(NBTTagCompound tagCompound) {
		for (int i = 0; i < unlockedLevels.length; i++) {
			unlockedLevels[i] = tagCompound.getByte("Unlocked" + i);
		}
		loadStories();
	}

	@Override
	public void receivePacket(DataReader dr, EntityPlayer player) {
		int story = dr.readByte();
		int level = dr.readByte();
		unlockedLevels[story] = level;
		if (unlockedLevels[story] > TrackStory.stories.get(story).getLevels().size() - 1) {
			unlockedLevels[story] = TrackStory.stories.get(story).getLevels().size() - 1;
		}
	}

	@Override
	public void checkGuiData(Object[] info) {
		for (int i = 0; i < unlockedLevels.length; i++) {
			updateGuiData(info, i, (short) unlockedLevels[i]);
		}
	}

	@Override
	public int numberOfGuiData() {
		return TrackStory.stories.size();
	}

	@Override
	public void receiveGuiData(int id, short data) {
		if (id >= 0 && id < unlockedLevels.length) {	
			unlockedLevels[id] = data;
			if (data != 0) {
				loadMaps();
			}else{
				loadStories();
			}
		}
	}	


	//editor stuff
	private TrackEditor editorTrack;
	private TrackDetector editorDetectorTrack;
	private Track hoveringTrack;
	private boolean isEditorTrackDraging;
	public void setEditorTrack(TrackEditor track) {
		if (editorTrack != null) {
			track.setType(editorTrack.getType());
		}
		editorTrack = track;
	}


	public void setEditorDetectorTrack(TrackDetector track) {
		if (track.equals(editorDetectorTrack)) {
			editorDetectorTrack = null;
		}else{
			editorDetectorTrack = track;
		}
	}	

	public TrackDetector getEditorDetectorTrack() {
		return editorDetectorTrack;
	}		

	private static final String VALID_SAVE_NAME_CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789 ";

	@Override
	public void keyPress(GuiVehicle gui, char character, int extraInformation) {
		if (isSaveMenuOpen) {
			if (saveName.length() < 15 && VALID_SAVE_NAME_CHARACTERS.indexOf(Character.toLowerCase(character)) != -1) {
				saveName += character;
			}else if (extraInformation == 14 && saveName.length() > 0){
				saveName = saveName.substring(0, saveName.length() - 1);
			}
		}else{
			if (!isUsingEditor() || isRunning) {
				return;
			}

			Track track;

			if (editorTrack != null) {
				track = editorTrack;
			}else{
				track = hoveringTrack;
			}


			switch (Character.toLowerCase(character)) {
				case 'a':
					if (track != null && track.getOrientation().getOpposite() != null ) {
						track.setOrientation(track.getOrientation().getOpposite());
					}
					break;
				case 'r':
					if (track != null) {
						for (TrackOrientation orientation : TrackOrientation.ALL) {
							if (orientation.getV() == track.getV()) {
								if ((orientation.getV() == 1 && orientation.getRotation() != track.getRotation()) || orientation.getRotation() == track.getRotation().getNextRotation()) {
									track.setOrientation(orientation);
									break;
								}
							}
						}
					}
					break;
				case 'f':
					if (track != null) {
						for (TrackOrientation orientation : TrackOrientation.ALL) {
							if (orientation.getV() == track.getV()) {
								if (orientation.getV() == 2 || orientation.getV() == 3) {
									if (orientation.getRotation() == track.getRotation().getFlippedRotation()) {
										track.setOrientation(orientation);
										break;
									}
								}
							}
						}
					}
					break;
				case 't':
					if (editorTrack != null) {
						editorTrack.nextType();
					}	
					break;
				case '1':
					setEditorTrack(new TrackEditor(TrackOrientation.CORNER_DOWN_RIGHT));
					break;
				case '2':
					setEditorTrack(new TrackEditor(TrackOrientation.STRAIGHT_VERTICAL));
					break;
				case '3':
					setEditorTrack(new TrackEditor(TrackOrientation.JUNCTION_3WAY_STRAIGHT_FORWARD_VERTICAL_CORNER_DOWN_RIGHT));
					break;
				case '4':
					setEditorTrack(new TrackEditor(TrackOrientation.JUNCTION_3WAY_CORNER_RIGHT_ENTRANCE_DOWN));
					break;
				case '5':
					setEditorTrack(new TrackEditor(TrackOrientation.JUNCTION_4WAY));
					break;
				case 'd':
					if (hoveringTrack != null) {
						tracks.remove(hoveringTrack);
						if (hoveringTrack.getX() >= 0 && hoveringTrack.getX() < trackMap.length && hoveringTrack.getY() >= 0 && hoveringTrack.getY() < trackMap[0].length) {
							trackMap[hoveringTrack.getX()][hoveringTrack.getY()] = null;
						}
						hoveringTrack = null;
					}
					break;
				case 'c':
					if (editorTrack == null && hoveringTrack != null) {
						setEditorTrack(new TrackEditor(hoveringTrack.getOrientation()));
						editorTrack.setType(hoveringTrack.getU());
					}
					break;
				case 's':
					if (hoveringTrack != null) {
						if (playerStartX == hoveringTrack.getX() && playerStartY == hoveringTrack.getY()) {
							playerStartDirection = playerStartDirection.getLeft();
						}else{
							playerStartX = hoveringTrack.getX();
							playerStartY = hoveringTrack.getY();
						}
						resetPosition();
					}

					break;

				case 'x':
					if (hoveringTrack != null) {
						itemX = hoveringTrack.getX();
						itemY = hoveringTrack.getY();
					}

					break;	
				case 'p':
					//TrackLevel.saveMap("Test", playerStartX, playerStartY, playerStartDirection, itemX, itemY, tracks);
					break;
			}
		}

	}


	private void handleEditorTrack(int x, int y, int button, boolean clicked) {
		if (isRunning) {
			isEditorTrackDraging = false;
			return;
		}

		if (editorTrack != null) {
			if ((clicked && button == 0) || (!clicked && button == -1 && isEditorTrackDraging)) {
				int x2 = x - LEFT_MARGIN;
				int y2 = y - TOP_MARGIN;

				int gridX = x2 / 16;
				int gridY = y2 / 16;

				if (gridX >= 0 && gridX < trackMap.length && gridY >= 0 && gridY < trackMap[0].length) {


					if (trackMap[gridX][gridY] == null) {
						Track newTrack = editorTrack.getRealTrack(gridX, gridY);
						trackMap[gridX][gridY] = newTrack;
						tracks.add(newTrack);
					}
					isEditorTrackDraging = true;
				}

			}else if (button == 1 || (!clicked && isEditorTrackDraging)) {
				if (clicked) {
					editorTrack = null;
				}
				isEditorTrackDraging = false;
			}
		}		
	}

	@Override
	public boolean disableStandardKeyFunctionality() {
		return isSaveMenuOpen;
	}	


	@SideOnly(Side.CLIENT)
	private boolean save(String name) {
		if (StevesVehicles.arcadeDevOperator) {
			if (name.startsWith(" ")) {
				name = name.substring(1);
			}else{
				String result = TrackLevel.saveMapToString(name, playerStartX, playerStartY, playerStartDirection, itemX, itemY, tracks);
				System.out.println(result);
				return true;
			}
		}

		if (TrackLevel.saveMap(name, playerStartX, playerStartY, playerStartDirection, itemX, itemY, tracks)) {
			lastSavedName = name;
			loadUserMaps();
			return true;
		}else{
			saveName = name;
			failedToSave = true;
			isSaveMenuOpen = true;

			return false;
		}
	}


}
