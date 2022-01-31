package life3d;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
/**
 * 
 * @author Donald A. Smith, ThinkerFeeler@gmail.com
 * 
 * Press SPACE to pause/continue, r to re-initialize, L to load a shape, q to quit,
 * 0 - 9 to choose an update rule, s for slower, f for faster, arrow keys or 
 * click and drag mouse to navigate.    Press + to increase maxExent, - to decrease maxExtent.
 * 
 * Run ShapeDesigner.java to design starting shapes.  
 * 
 * TODO: have probabilistic rules.
 * TODO: draw different shapes
 * TODO: compute 4D life and project to 3d
 * TODO: make update depend partially on larger neighborhoods.
 */
public class Life3D extends Application {
	private int maxExtent = 16; // This defines the maximum offset allowed.
	public static String initialDirectory="shapes"; // set to something like "c:/tmp/" to load from that directory
	private static boolean useRandomMaterials = false; // false makes it use hsb
														// colors
	// surviveLow,surviveHigh, bornLow,bornHigh
	private static final int[][] rules =
			{ { 1, 3, 2, 3 }, // 0
			{ 2, 3, 2, 3 }, // 1
			{ 2, 3, 3, 3 }, // 2
			{ 2, 2, 3, 3 }, // 3
			{ 3, 3, 2, 2 }, // 4
			{ 3, 3, 1, 1 }, // 5
			{ 2, 2, 1, 1 }, // 6
			{ 1, 1, 1, 1 }, // 7
			{ 1, 1, 2, 2 }, // 8
			{ 0, 0, 1, 1 }, // 9
			{ 1, 2, 0, 0 }, // 10
			{ 3, 4, 1, 3 }, // 11
			{19,20,3,5}, // 12
			{ 3, 4, 1, 3 }, // random
	};
	private static int ruleIndex = 5;

	private final static int NUMBER_OF_RANDOM_COLORS = 200;
	private final static int NUMBER_OF_HSB_COLORS = 20;
	private final static int width = 1600;
	private final static int height = 900;
	private static final long ONE_MILLISECOND_IN_NANOSECONDS = 1000L * 1000L;
	private static final long ONE_SECOND_IN_NANOSECONDS = 1000L * 1000L * 1000L;
	private final Group root = new Group();
	private final XformWorld world = new XformWorld();
	private final PerspectiveCamera camera = new PerspectiveCamera(true);
	private final XformCamera cameraXform = new XformCamera();
	private static double cameraInitialZ = -500;
	private static double cameraInitialY = 0;
	private static final double CAMERA_NEAR_CLIP = 0.1;
	private static final double CAMERA_FAR_CLIP = 20000.0;
	private static Random random = new Random();
	private double mousePosX, mousePosY, mouseOldX, mouseOldY, mouseDeltaX, mouseDeltaY;
	private PhongMaterial sphereMaterial1 = new PhongMaterial();
	private PhongMaterial sphereMaterial2 = new PhongMaterial();
	private static final Point3D YAXIS = new Point3D(0, 1, 0);
	private static List<PhongMaterial> randomMaterials = new ArrayList<>(NUMBER_OF_RANDOM_COLORS);
	private static List<PhongMaterial> hsbMaterials = new ArrayList<>(NUMBER_OF_HSB_COLORS);
	private volatile Sphere selectedSphere = null;
	private volatile boolean isRunning = true;
	private PointLight light1;
	private PointLight light2;
	private Set<Shape3D> shapes = new HashSet<Shape3D>();
	private Map<Integer, Map<Integer, Map<Integer, Shape3D>>> shapeMap = new HashMap<Integer, Map<Integer, Map<Integer, Shape3D>>>();
	private Stage primaryStage;
	private int speedInMls = 1600;

	// --------------
	static {
		randomizeColorsAndMaterials();
	}

	// ---------------------------------
	private static void randomizeColorsAndMaterials() {
		randomMaterials.clear();
		for (int i = 0; i < NUMBER_OF_RANDOM_COLORS; i++) {
			Color color = randomColorExpensive();
			PhongMaterial pm = new PhongMaterial();
			// pm.setSpecularColor(randomColorExpensive());
			pm.setDiffuseColor(color);
			randomMaterials.add(pm);
		}
		for (int i = 0; i < NUMBER_OF_HSB_COLORS; i++) {
			Color color = Color.hsb((360.0 * i) / NUMBER_OF_HSB_COLORS, 0.8, 0.8);
			// color = new
			// Color(color.getRed(),color.getGreen(),color.getBlue(),0.7);
			PhongMaterial pm = new PhongMaterial();
			pm.setDiffuseColor(color);
			hsbMaterials.add(pm);
		}
	}

	// --------------------------
	private Shape3D getShape(Integer i, Integer j, Integer k) {
		Map<Integer, Map<Integer, Shape3D>> map2 = shapeMap.get(i);
		if (map2 == null) {
			return null;
		}
		Map<Integer, Shape3D> map3 = map2.get(j);
		if (map3 == null) {
			return null;
		}
		return map3.get(k);
	}

	// --------------------------
	private boolean deleteShape(Integer i, Integer j, Integer k) {
		Map<Integer, Map<Integer, Shape3D>> map2 = shapeMap.get(i);
		if (map2 == null) {
			return false;
		}
		Map<Integer, Shape3D> map3 = map2.get(j);
		if (map3 == null) {
			return false;
		}
		boolean result = map3.remove(k) != null;
		if (map3.isEmpty()) {
			map2.remove(j);
			if (map2.isEmpty()) {
				shapeMap.remove(i);
			}
		}
		return result;
	}

	// ------------------------
	private void setShape(Integer i, Integer j, Integer k, Shape3D shape) {
		Map<Integer, Map<Integer, Shape3D>> map2 = shapeMap.get(i);
		if (map2 == null) {
			map2 = new HashMap<Integer, Map<Integer, Shape3D>>();
			shapeMap.put(i, map2);
		}
		Map<Integer, Shape3D> map3 = map2.get(j);
		if (map3 == null) {
			map3 = new HashMap<Integer, Shape3D>();
			map2.put(j, map3);
		}
		map3.put(k, shape);
	}

	// -------------------------
	private static class XformWorld extends Group {
		final Translate t = new Translate(0.0, 0.0, 0.0);
		final Rotate rx = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
		final Rotate ry = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
		final Rotate rz = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);

		public XformWorld() {
			super();
			this.getTransforms().addAll(t, rx, ry, rz);
			rx.setAngle(5);
			ry.setAngle(40);
			rz.setAngle(-1);
		}
	}

	// -------------------------
	private static class XformCamera extends Group {
		final Translate t = new Translate(0.0, 0.0, 0.0);
		final Rotate rx = new Rotate(16, 0, 0, 0, Rotate.X_AXIS);
		final Rotate ry = new Rotate(-15, 0, 0, 0, Rotate.Y_AXIS);
		final Rotate rz = new Rotate(-5, 0, 0, 0, Rotate.Z_AXIS);

		public XformCamera() {
			super();
			this.getTransforms().addAll(t, rx, ry, rz);
		}
	}

	private void buildCamera() {
		root.getChildren().add(cameraXform);
		cameraXform.getChildren().add(camera);
		camera.setNearClip(CAMERA_NEAR_CLIP);
		camera.setFarClip(CAMERA_FAR_CLIP);
		camera.setTranslateZ(cameraInitialZ);
		camera.setTranslateY(cameraInitialY);
		camera.setRotationAxis(YAXIS);
	}

	private void doReleaseOrMouseExit() {
		if (selectedSphere != null) {
			selectedSphere.setScaleX(1);
			selectedSphere.setScaleY(1);
			selectedSphere.setScaleZ(1);
			selectedSphere = null;
		}
	}

	private void addLights() {
		light1 = new PointLight(new Color(0.9, 0.6, 0.6, 1));
		light1.setTranslateX(-400);
		light1.setTranslateY(300);
		light1.setTranslateZ(-2000);
		world.getChildren().add(light1);

		light2 = new PointLight(new Color(0.5, 0.9, 0.8, 1));
		light2.setTranslateX(500);
		light2.setTranslateY(-300);
		light2.setTranslateZ(-500);
		world.getChildren().add(light2);

		AmbientLight ambientLight = new AmbientLight(new Color(0.6, 0.3, 0.4, 1));
		world.getChildren().add(ambientLight);
	}

	private static Color randomColorExpensive() {
		return new Color(random.nextDouble(), random.nextDouble(), random.nextDouble(), 0.7);
	}

	private void handleMouse(Scene scene) {
		final double bigSize = 10;
		scene.setOnMousePressed((MouseEvent me) -> {
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseOldX = me.getSceneX();
			mouseOldY = me.getSceneY();
			// this is done after clicking and the rotations are apparently
			// performed in coordinates that are NOT rotated with the camera.
			// (pls activate the two lines below for clicking)
			// cameraXform.rx.setAngle(-90.0);
			// cameraXform.ry.setAngle(180.0);
			PickResult pr = me.getPickResult();
			if (pr.getIntersectedNode() instanceof Sphere) {
				// selectedSphere = (Sphere) pr.getIntersectedNode();
				// selectedSphere.setScaleX(bigSize);
				// selectedSphere.setScaleY(bigSize);
				// selectedSphere.setScaleZ(bigSize);
			}
			if (pr.getIntersectedNode() instanceof Cylinder) {
			}
		});
		scene.setOnMouseReleased((MouseEvent me) -> {
			doReleaseOrMouseExit();
		});
		scene.setOnMouseDragExited((MouseEvent me) -> {
			doReleaseOrMouseExit();
		});
		scene.setOnMouseDragged((MouseEvent me) -> {
			mouseOldX = mousePosX;
			mouseOldY = mousePosY;
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseDeltaX = (mousePosX - mouseOldX);
			mouseDeltaY = (mousePosY - mouseOldY);
			if (me.isPrimaryButtonDown()) {
				// this is done when the mouse is dragged and each rotation is
				// performed in coordinates, that are rotated with the camera.
				// cameraXform.ry.setAngle(cameraXform.ry.getAngle() +
				// mouseDeltaX * 0.2);
				// cameraXform.rx.setAngle(cameraXform.rx.getAngle() -
				// mouseDeltaY * 0.2);

				world.ry.setAngle(world.ry.getAngle() - mouseDeltaX * 0.2);
				world.rx.setAngle(world.rx.getAngle() + mouseDeltaY * 0.2);
			} else if (me.isSecondaryButtonDown()) {
				cameraXform.t.setZ(cameraXform.t.getZ() - mouseDeltaY);
				cameraXform.t.setX(cameraXform.t.getX() - mouseDeltaX);
			}
		});
	}

	private void handleKeyEvents(Scene scene) {
		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
			public void handle(KeyEvent ke) {
				switch (ke.getCode()) {
				case SPACE:
					isRunning = !isRunning;
					break;
				case Q:
					System.exit(0);
					break;
				case L:
					try {
						load();
						}
					catch (IOException exc) {
						exc.printStackTrace();
						MessageBox.show(exc.getMessage(), "Unable to load shape");
					}
				case DIGIT0:
					ruleIndex = 0;
					makeTitle();
					break;
				case DIGIT1:
					ruleIndex = 1;
					makeTitle();
					break;
				case PAGE_UP:
					maxExtent++;
					System.out.println("Set maxExtent to " + maxExtent);
					break;
				case PAGE_DOWN:
					if (maxExtent>5) {
						maxExtent--;
						System.out.println("Set maxExtent to " + maxExtent);
					}
					break;
				case DIGIT2:
					ruleIndex = 2;
					makeTitle();
					break;
				case DIGIT3:
					ruleIndex = 3;
					makeTitle();
					break;
				case DIGIT4:
					ruleIndex = 4;
					makeTitle();
					break;
				case DIGIT5:
					ruleIndex = 5;
					makeTitle();
					break;
				case DIGIT6:
					ruleIndex = 6;
					makeTitle();
					break;
				case DIGIT7:
					ruleIndex = 7;
					makeTitle();
					break;
				case DIGIT8:
					ruleIndex = 8;
					makeTitle();
					break;
				case DIGIT9:
					ruleIndex = 9;
					makeTitle();
					break;
				case MINUS:
					ruleIndex = 10;
					makeTitle();
					break;
				case EQUALS:
					ruleIndex = 11;
					makeTitle();
					break;
				case BACK_SPACE:
					ruleIndex = 12;
					makeTitle();
					break;
				case V:
					cameraXform.t.setZ(0);
					cameraXform.rx.setAngle(0);
					cameraXform.ry.setAngle(0);
					camera.setTranslateX(0);
					camera.setTranslateY(cameraInitialY);
					camera.setTranslateZ(cameraInitialZ);
					break;
				case LEFT:
					camera.setTranslateX(camera.getTranslateX() + 10);
					break;
				case RIGHT:
					camera.setTranslateX(camera.getTranslateX() - 10);
					break;
				case UP:
					if (ke.isShiftDown()) {
						camera.setTranslateY(camera.getTranslateY() + 10);
					} else {
						camera.setTranslateZ(camera.getTranslateZ() + 10);
					}
					break;
				case DOWN:
					if (ke.isShiftDown()) {
						camera.setTranslateY(camera.getTranslateY() - 10);
					} else {
						camera.setTranslateZ(camera.getTranslateZ() - 10);
					}
					break;
				case C:					
					randomizeColorsAndMaterials();
					updateColors();
					world.requestLayout();
					break;
				case R:
					if (ke.isShiftDown()) {
						randomRule();
						ruleIndex = rules.length-1;
					}
					world.getChildren().removeAll(shapes);
					shapes.clear();
					shapeMap.clear();
					draw();
					isRunning=true;
					break;
				case S:
					speedInMls = (int) (1.1 * speedInMls);
					break;
				case F:
					speedInMls = (int) (speedInMls / 1.1);
					break;
				default:
				}
			}
		});
	}
	
	private void load() throws IOException {
		isRunning=false;
		JFileChooser chooser = new JFileChooser();
		if (initialDirectory!=null) {
			chooser.setCurrentDirectory(new File(initialDirectory));
		}
		FileFilter filter = new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.getName().endsWith(".shape");
			}

			@Override
			public String getDescription() {
				return "Shape files";
			}};
		chooser.setFileFilter(filter);
		int result=chooser.showOpenDialog(null);
		if (result!= JFileChooser.APPROVE_OPTION) {
			isRunning=true;
		} else {
			File file=chooser.getSelectedFile();
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			List<int[]> listOfLocations = new ArrayList<>();
			while (true) {
				String line=reader.readLine();
				if (line==null) {
					break;
				}
				line=line.trim();
				if (line==null || line.startsWith("#")) {
					continue;
				}
				String parts[] = line.split(",");
				if (parts.length!=3) {
					MessageBox.show("Unexpected line: " + line + " in " + file,"Unable to load shape");
					reader.close();
					isRunning=true;
					return;
				}
				int x= Integer.parseInt(parts[0]);
				int y= Integer.parseInt(parts[1]);
				int z= Integer.parseInt(parts[2]);
				listOfLocations.add(new int[] {x,y,z});
			} // while
			reader.close();
			world.getChildren().removeAll(shapes);
			shapes.clear();
			shapeMap.clear();
			for(int[] shape: listOfLocations) {
				draw(shape[0],shape[1],shape[2]);
			}
			makeTitle();
			isRunning=false;
		}
	}

	private void scaleRadius(Shape3D shape, double scale) {
		if (shape instanceof Cylinder) {
			Cylinder cylinder = (Cylinder) shape;
			cylinder.setRadius(scale * cylinder.getRadius());
		} else if (shape instanceof Sphere) {
			shape.setScaleX(shape.getScaleX() * scale);
			shape.setScaleY(shape.getScaleY() * scale);
			shape.setScaleZ(shape.getScaleZ() * scale);
		}
	}

	private int countNeighbors(Shape3D shape) {
		int i = getI(shape);
		int j = getJ(shape);
		int k = getK(shape);
		return countNeighbors(i, j, k);
	}

	private int countNeighbors(final int i, final int j, final int k) {
		int count = 0;
		for (int deltaX = -1; deltaX < 2; deltaX++) {
			Map<Integer, Map<Integer, Shape3D>> map2 = shapeMap.get(i + deltaX);
			if (map2 == null) {
				continue;
			}
			for (int deltaY = -1; deltaY < 2; deltaY++) {
				Map<Integer, Shape3D> map3 = map2.get(j + deltaY);
				if (map3 == null) {
					continue;
				}
				for (int deltaZ = -1; deltaZ < 2; deltaZ++) {
					if (deltaX != 0 || deltaY != 0 || deltaZ != 0) {
						if (map3.get(k + deltaZ) != null) {
							count++;
						}
					}
				}
			}
		}
		return count;
	}

	private void updateColors() {
		for (Shape3D shape : shapes) {
			int i = getI(shape);
			int j = getJ(shape);
			int k = getK(shape);
			int distance = Math.abs(i) + Math.abs(j) + Math.abs(k); // Manhattan
																	// distance
			// System.out.println(distance);
			PhongMaterial material = randomMaterials.get(distance % randomMaterials.size());
			shape.setMaterial(material);
		}
	}

	private void draw(int i, int j, int k) {
		// Sphere shape = new Sphere(3);
		Box shape = new Box(7, 7, 7);
		// shape.setRotationAxis(new
		// Point3D(oneOrMinusOne(),oneOrMinusOne(),oneOrMinusOne()));
		// shape.setRotate(360*random.nextDouble());
		int distance = // Math.abs(i) + Math.abs(j) + Math.abs(k); // Manhattan
						// distance
				(int) (5 * Math.sqrt(square(i) + square(j) + square(k)));
		// System.out.println(distance);
		PhongMaterial material;
		int materialIndex;
		if (useRandomMaterials) {
			materialIndex = distance % randomMaterials.size();
			material = randomMaterials.get(materialIndex);
		} else {
			materialIndex = distance % hsbMaterials.size();
			material = hsbMaterials.get(materialIndex);
		}
		// shape.setUserData(new int[] {materialIndex});
		shape.setMaterial(material);
		shape.setTranslateX(i * 10);
		shape.setTranslateY(j * 10);
		shape.setTranslateZ(k * 10);
		world.getChildren().add(shape);
		setShape(i, j, k, shape);
		shapes.add(shape);
	}

	private double oneOrMinusOne() {
		return random.nextBoolean() ? -1.0 : 1.0;
	}

	private static double square(double x) {
		return x * x;
	}

	private static class IJK {
		int i, j, k;

		public IJK(int i, int j, int k) {
			this.i = i;
			this.j = j;
			this.k = k;
		}

		@Override
		public boolean equals(Object obj) {
			IJK other = (IJK) obj;
			return i == other.i && j == other.j && k == other.k;
		}

		@Override
		public int hashCode() {
			return i * 17 + j + 19 + k;
		}
	}

	private static int getI(Shape3D shape) {
		return (int) Math.round(shape.getTranslateX() / 10);
	}

	private static int getJ(Shape3D shape) {
		return (int) Math.round(shape.getTranslateY() / 10);
	}

	private static int getK(Shape3D shape) {
		return (int) Math.round(shape.getTranslateZ() / 10);
	}

	private void update() {
		final List<Shape3D> shapesToDelete = new ArrayList<Shape3D>();
		final int rule[] = rules[ruleIndex];
		// Survive?
		for (Shape3D shape : shapes) {
			int count = countNeighbors(shape);
			if (count >= rule[0] && count <= rule[1]) {
				// survive
			} else {
				shapesToDelete.add(shape);
			}
			
		}
		// Born?
		final List<IJK> ijksToBeBorn = new ArrayList<IJK>();
		for (int x = -maxExtent; x <= maxExtent; x++) {
			for (int y = -maxExtent; y <= maxExtent; y++) {
				for (int z = -maxExtent; z <= maxExtent; z++) {
					if (getShape(x,y,z)!=null) {
						continue;
					}
					int neighbors = countNeighbors(x, y, z);
					if (neighbors >= rule[2] && neighbors <= rule[3]) {
						int total = shapes.size() + ijksToBeBorn.size();
						if (total < 1000000 || random.nextInt(500000) > total) {
							ijksToBeBorn.add(new IJK(x, y, z));
						}
						// System.out.println("Adding " + i + " " + j + " " + k);
					}
				}
			}
		}
		// Delete dead ones
		for (Shape3D shape : shapesToDelete) {
			int i = getI(shape);
			int j = getJ(shape);
			int k = getK(shape);
			deleteShape(i, j, k);
			shapes.remove(shape);
			world.getChildren().remove(shape);
		}
		// Add newones:
		for (IJK ijk : ijksToBeBorn) {
			draw(ijk.i, ijk.j, ijk.k);
		}
		System.out.println(
				"Added " + ijksToBeBorn.size() + " deleting " + shapesToDelete.size() + ", #shapes = " + shapes.size());
	}

	private void drawRandom(int range) {
		int halfRange = range / 2;
		for (int c = 0; c < 4; c++) {
			int i = random.nextInt(range) - halfRange;
			int j = random.nextInt(range) - halfRange;
			int k = random.nextInt(range) - halfRange;
			if (getShape(i, j, k) == null) {
				draw(i, j, k);
			}
		}
	}

	private static NumberFormat numberFormat = NumberFormat.getInstance();
	private static String format(int number) {
		StringBuilder sb=new StringBuilder();
		String formatted=numberFormat.format(number);
		int cnt=8-formatted.length();
		for(int i=0;i<cnt;i++) {
			sb.append(' ');
		}
		sb.append(formatted);
		return sb.toString();
	}

	private static final String HELP_MESSAGE="Press SPACE to pause/continue, r for random simple shape, R for random rule, L to load shape, q to quit, "
			+ "0 - 9 to choose an update rule, s for slower, f for faster, arrow keys or click and drag mouse to navigate.";
	private void makeTitle() {
		primaryStage.setTitle("Life3D:  " + HELP_MESSAGE
				+ "   Rule " + ruleIndex + " with "
				+ format(shapes.size()) + " nodes");
	}

	private void drawStar1() {
		/*
		 * + + + +
		 */
		draw(0, 1, 0);
		draw(0, -1, 0);
		draw(-1, 0, 0);
		draw(1, 0, 0);
	}

	private void drawStar2() {
		/*
		 * + +++ +
		 */
		draw(0, 1, 0);
		draw(0, -1, 0);
		draw(0, 0, 0);
		draw(-1, 0, 0);
		draw(1, 0, 0);
	}

	private void drawSquare0() {
		/*
		 * + + + +
		 */
		draw(0, 0, 0);
		draw(0, 1, 0);
		draw(1, 0, 0);
		draw(1, 1, 0);
	}

	private void drawSquare1() {
		/*
		 * ++
		 * 
		 * ++
		 */
		draw(0, 0, 0);
		draw(0, 1, 0);
		draw(2, 0, 0);
		draw(2, 1, 0);
	}

	private void drawSquare2() {
		/*
		 * + +
		 * 
		 * + +
		 */
		draw(-1, 0, 0);
		draw(-1, 2, 0);
		// draw(1,1,0);
		draw(1, 0, 0);
		draw(1, 2, 0);
	}

	private void drawSquare3() {
		/*
		 * + + + + +
		 */
		draw(-1, -1, 0);
		draw(-1, 1, 0);
		draw(0, 0, 0);
		draw(1, -1, 0);
		draw(1, 1, 0);
	}

	private void drawOffset1() {
		/*
		 * + +
		 * 
		 * + +
		 */
		draw(0, 0, 0);
		draw(0, 2, 0);
		// draw(1,1,0);
		draw(2, 1, 0);
		draw(2, 3, 0);
	}

	private void drawLine1() {
		/*
		 * +++
		 */
		draw(0, -1, 0);
		draw(0, 0, 0);
		draw(0, 1, 0);
	}

	private void drawLine2() {
		/*
		 * + + +
		 */
		draw(0, -2, 0);
		draw(0, 0, 0);
		draw(0, 2, 0);
	}

	private void drawLineCircle1() {
		/*
		 * + + + + + +
		 */
		draw(0, 0, 0);
		draw(1, -1, 0);
		draw(1, 1, 0);
		draw(2, -1, 0);
		draw(2, 1, 0);
		draw(3, 0, 0);
	}

	private void drawLineCircle2() {
		/*
		 * ++ + + + + ++
		 */
		draw(0, 0, 0);
		draw(0, 1, 0);
		draw(1, -1, 0);
		draw(1, 2, 0);
		draw(2, -1, 0);
		draw(2, 2, 0);
		draw(3, 0, 0);
		draw(3, 1, 0);
	}

	private void drawRectangle() {
		/*
		 * + + + + + +
		 */
		draw(-1, -1, 0);
		draw(-1, 1, 0);
		draw(0, -1, 0);
		draw(0, 1, 0);
		draw(1, -1, 0);
		draw(1, 1, 0);
	}

	private void draw3DFour1() {
		draw(0, 0, 0);
		draw(0, 0, 1);
		draw(0, 1, 0);
		draw(0, 1, 1);
		draw(1, 0, 0);
		draw(1, 0, 1);
		draw(1, 1, 0);
		draw(1, 1, 1);
	}

	private void draw3DFour2() {
		draw(0, 0, 0);
		draw(-1, -1, -1);
		draw(-1, -1, 1);
		draw(-1, 1, -1);
		draw(-1, 1, 1);
		draw(1, -1, -1);
		draw(1, -1, 1);
		draw(1, 1, -1);
		draw(1, 1, 1);
	}

	private void drawLittleSphere1() {
		int[] list1 = { -1, 0, 1 };
		for (int i : list1) {
			for (int j : list1) {
				for (int k : list1) {
					if (i != 0 || j != 0 || k != 0) {
						draw(i, j, k);
					}
				}
			}
		}
	}

	private void drawLittleSphere2() {
		int[] list1 = { -2, 0, 2 };
		for (int i : list1) {
			for (int j : list1) {
				for (int k : list1) {
					if (i != 0 || j != 0 || k != 0) {
						draw(i, j, k);
					}
				}
			}
		}
	}

	private void randomRule() {
		int surviveLow = random.nextInt(20);
		int surviceHigh= surviveLow + random.nextInt(24-surviveLow);
		int bornLow = random.nextInt(20);
		int bornHigh= bornLow + random.nextInt(24-surviveLow);
		int[] rule = {surviveLow,surviceHigh,bornLow, bornHigh};
		System.out.println(Arrays.toString(rule));
		rules[rules.length-1] = rule;
	}
	private void draw() {
		switch (random.nextInt(17)) {
		case 0:
			drawStar1();
			break;
		case 1:
			drawStar2();
			break;
		case 2:
			drawSquare0();
			break;
		case 3:
			drawSquare1();
			break;
		case 4:
			drawSquare2();
			break;
		case 5:
			drawSquare3();
			break;
		case 6:
			drawRandom(6);
			break;
		case 7:
			drawOffset1();
			break;
		case 8:
			drawLine1();
			break;
		case 9:
			drawLine2();
			break;
		case 10:
			drawLineCircle1();
			break;
		case 11:
			drawLineCircle2();
			break;
		case 12:
			draw3DFour1();
			break;
		case 13:
			draw3DFour2();
			break;
		case 14:
			drawRectangle();
			break;
		case 15:
			drawLittleSphere1();
			break;
		case 16:
			drawLittleSphere2();
			break;
		}
		makeTitle();
	}

	// ------------
	private void animate() {
		final long last[] = { 0 };
		final AnimationTimer timer = new AnimationTimer() {
			@Override
			public void handle(long nowInNanoSeconds) {
				if (!isRunning) {
					return;
				}
				cameraXform.rz.setAngle(cameraXform.rz.getAngle() + 0.2);
				// running=false;
				if (nowInNanoSeconds - last[0] < speedInMls * ONE_MILLISECOND_IN_NANOSECONDS) {
					return;
				}
				last[0] = nowInNanoSeconds;
				update();
				makeTitle();
				world.requestLayout();
				if (shapes.isEmpty()) {
					isRunning=false;
				}
			}
		};
		timer.start();
	}

	// ------------
	@Override
	public void start(Stage stage) throws Exception {
		primaryStage = stage;
		sphereMaterial1.setDiffuseColor(new Color(1, 0, 0, 1.0));
		sphereMaterial2.setDiffuseColor(Color.BLUE);
		root.getChildren().add(world);
		root.setDepthTest(DepthTest.ENABLE);
		Scene scene = new Scene(root, width, height, true);
		scene.setFill(Color.DIMGREY);
		primaryStage.setTitle("Life3D");
		primaryStage.setScene(scene);
		handleMouse(scene);
		handleKeyEvents(scene);
		buildCamera();
		addLights();
		scene.setCamera(camera);
		primaryStage.show();
		// drawSquare3();
		drawLittleSphere1();
		animate();
		makeTitle();
	}

	public static void main(String[] args) {
		try {
			launch(args);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}
}
