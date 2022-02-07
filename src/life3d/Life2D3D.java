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
 * Life2D but showing history in 3D.
 * 
 * Press SPACE to pause/continue, r to re-initialize, L to load a shape, q to quit,
 * 0 - 9 to choose an update rule, s for slower, f for faster, arrow keys or 
 * click and drag mouse to navigate.    Press PAGE_UP to increase maxExent, PAGE_DOWN to decrease maxExtent.
 * 
 * Run ShapeDesigner.java to design starting shapes.  
 * 
 * TODO: have probabilistic rules.
 * TODO: draw different shapes
 * TODO: compute 4D life and project to 3d
 * TODO: make update depend partially on larger neighborhoods.
 */
public class Life2D3D extends Application {
	private int maxExtent = 16; // This defines the maximum number of positions allowed in each direction x, y, and z.
	// So, maxExtent = 16 means there are 16*16*16 = 4096 positions for cubes.
	private int halfMaxExtent = maxExtent/2;
	// Shapes displayed on the screen are centered at (0,0,0) and x,y,z range from -maxExtent to +maxExtent inclusive.
	// But in the matrix below, indices range from 0 to maxExtent inclusive.
	private Shape3D[][] matrix2D = new Shape3D[maxExtent+1][maxExtent+1];

	public static String initialDirectory="shapes"; // set to something like "c:/tmp/" to load from that directory
	private static boolean useRandomMaterials = false; // false makes it use hsb colors
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
	private Stage primaryStage;
	private int delayBetweenStepsInMls = 800;
	private int z = 0;
	private PhongMaterial material = new PhongMaterial(Color.hsb(0, 1.0, 1.0,0.5));
	// --------------

	// --------------------------
	private Shape3D getShape(int i, int j) {
		i+= halfMaxExtent; j+=halfMaxExtent;
		return matrix2D[i][j];
	}

	// --------------------------
	private void deleteShape(int i, int j) {
		i+= halfMaxExtent; j+=halfMaxExtent;
		matrix2D[i][j]= null;
	}

	// ------------------------
	private void setShape(int i, int j, Shape3D shape) {
		i+= halfMaxExtent; j+=halfMaxExtent; 
		matrix2D[i][j]= shape;
	}

	// -------------------------
	private class XformWorld extends Group {
		final Translate t = new Translate(0.0, 0.0, 0.0);
//		final Rotate rx = new Rotate(0, extentOffset, extentOffset, extentOffset, Rotate.X_AXIS);
//		final Rotate ry = new Rotate(0, extentOffset, extentOffset, extentOffset, Rotate.Y_AXIS);
//		final Rotate rz = new Rotate(0, extentOffset, extentOffset, extentOffset, Rotate.Z_AXIS);
		final Rotate rx = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
		final Rotate ry = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
		final Rotate rz = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);
		public XformWorld() {
			super();
			this.getTransforms().addAll(t, rx, ry, rz);
		}
	}

	// -------------------------
	private static class XformCamera extends Group {
		final Translate t = new Translate(400.0, 0.0, 0.0);
		final Rotate rx = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
		final Rotate ry = new Rotate(-49, 0, 0, 0, Rotate.Y_AXIS);
		final Rotate rz = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);

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
					clear();
					maxExtent+=2;
					halfMaxExtent++;
					System.out.println("Set maxExtent to " + maxExtent);
					matrix2D = new Shape3D[maxExtent+1][maxExtent+1];
					drawLittleSphere2();
					break;
				case PAGE_DOWN:
					if (maxExtent>7) {
						clear();
						maxExtent-=2;
						halfMaxExtent--;
						matrix2D = new Shape3D[maxExtent+1][maxExtent+1];
						drawLittleSphere2();
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
				case R:
					if (ke.isShiftDown()) {
						randomRule();
						ruleIndex = rules.length-1;
					}
					clear();
					draw();
					isRunning=true;
					break;
				case S:
					delayBetweenStepsInMls = (int) (1.1 * delayBetweenStepsInMls);
					break;
				case F:
					delayBetweenStepsInMls = (int) (delayBetweenStepsInMls / 1.1);
					break;
				default:
				}
			}
		});
	}
	private void clear() {
		z = 0;
		world.getChildren().clear();
		shapes.clear();
		clearShapesMatrix();
	}
	private void clearShapesMatrix() {
		for(int i=0;i<maxExtent;i++) {
			for(int j=0;j<maxExtent;j++) {
				matrix2D[i][j]=null;
			}
		}
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
				listOfLocations.add(new int[] {x,y});
			} // while
			reader.close();
			clear();
			for(int[] shape: listOfLocations) {
				draw(shape[0],shape[1]);
			}
			makeTitle();
			isRunning=false;
		}
	}

	private int countNeighbors(Shape3D shape) {
		int i = getI(shape);
		int j = getJ(shape);
		return countNeighbors(i+halfMaxExtent, j+halfMaxExtent);
	}

	// i, j, k are indices into matrix
	private int countNeighbors(final int i, final int j) {
		int count = 0;
		for (int deltaX = -1; deltaX < 2; deltaX++) {
			final int ii=i+deltaX;
			if (ii<0 || ii> maxExtent) {
				continue;
			}
			for (int deltaY = -1; deltaY < 2; deltaY++) {
				final int jj= j+deltaY;
				if (jj<0 || jj> maxExtent) {
					continue;
				}
				if (matrix2D[ii][jj]!=null) {
					count++;
				}
			}
		}
		return count;
	}

	// i, j, and k are between -halfMaxExtent and halfMaxExtent, inclusive
	private void draw(int i, int j) {
		assert(i>= -halfMaxExtent && i<= halfMaxExtent);
		assert(j>= -halfMaxExtent && j<= halfMaxExtent);
		//System.out.println("draw(" + i + ", " + j + ", " + k + ")");
		if (getShape(i,j)!=null) {
			System.err.println("Warning: shape already exists at " + i + ", " + j);
			return;
		}
		Box shape = new Box(7, 7, 7);
		final int distance = (int) (5 * Math.sqrt(square(i) + square(j)));
		shape.setMaterial(material);
		shape.setTranslateX(i * 10);
		shape.setTranslateY(j * 10);
		shape.setTranslateZ(z);
		world.getChildren().add(shape);
		setShape(i, j, shape);
		shapes.add(shape);
	}
	private int getI(Shape3D shape) {
		return (int) Math.round(shape.getTranslateX() / 10);
	}

	private int getJ(Shape3D shape) {
		return (int) Math.round(shape.getTranslateY() / 10);
	}

	private static double square(double x) {
		return x * x;
	}

	private static class IJ {
		int i, j;

		public IJ(int i, int j) {
			this.i = i;
			this.j = j;
		}

		@Override
		public boolean equals(Object obj) {
			IJ other = (IJ) obj;
			return i == other.i && j == other.j;
		}

		@Override
		public int hashCode() {
			return i * 17 + j*19;
		}
	}

	private void update() {
		final List<Shape3D> shapesToDelete = new ArrayList<Shape3D>();
		final int rule[] = rules[ruleIndex];
		// Survive?
		if (shapes.isEmpty()) {
			return;
		}
		for (Shape3D shape : shapes) {
			int count = countNeighbors(shape);
			if (count >= rule[0] && count <= rule[1]) {
				// survive
			} else {
				shapesToDelete.add(shape);
			}
			
		}
		// Born?
		final List<IJ> ijsToBeBorn = new ArrayList<IJ>();
		for (int x = -halfMaxExtent;  x <= halfMaxExtent; x++) {
			for (int y = -halfMaxExtent;  y <= halfMaxExtent; y++) {
					if (getShape(x,y)!=null) {
						continue;
					}
					int neighbors = countNeighbors(x+halfMaxExtent, y+halfMaxExtent);
					if (neighbors >= rule[2] && neighbors <= rule[3]) {
						int total = shapes.size() + ijsToBeBorn.size();
						if (total < 1000000 || random.nextInt(500000) > total) {
							ijsToBeBorn.add(new IJ(x, y));
						}
				}
			}
		}
		// Delete dead ones
		for (Shape3D shape : shapesToDelete) {
			int i = getI(shape);
			int j = getJ(shape);
			deleteShape(i, j);
			shapes.remove(shape);
		}
		z+= 15;
		material = new PhongMaterial(Color.hsb(3*z, 0.8, 1.0,0.5));
		// Add newones:
		for (IJ ij : ijsToBeBorn) {
			draw(ij.i, ij.j);
		}
		System.out.println(
				"Added " + ijsToBeBorn.size() + " deleting " + shapesToDelete.size() + ", #shapes = " + shapes.size());
	}

	private void drawRandom(int range) {
		int halfRange = range / 2;
		for (int c = 0; c < 4; c++) {
			int i = random.nextInt(range) - halfRange;
			int j = random.nextInt(range) - halfRange;
			if (getShape(i, j) == null) {
				draw(i, j);
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
		draw(0, 1);
		draw(0, -1);
		draw(-1, 0);
		draw(1, 0);
	}

	private void drawStar2() {
		/*
		 * + +++ +
		 */
		draw(0, 1);
		draw(0, -1);
		draw(0, 0);
		draw(-1, 0);
		draw(1, 0);
	}

	private void drawSquare0() {
		/*
		 * + + + +
		 */
		draw(0, 0);
		draw(0, 1);
		draw(1, 0);
		draw(1, 1);
	}

	private void drawSquare1() {
		/*
		 * ++
		 * 
		 * ++
		 */
		draw(0, 0);
		draw(0, 1);
		draw(2, 0);
		draw(2, 1);
	}

	private void drawSquare2() {
		/*
		 * + +
		 * 
		 * + +
		 */
		draw(-1, 0);
		draw(-1, 2);
		// draw(1,1,0);
		draw(1, 0);
		draw(1, 2);
	}

	private void drawSquare3() {
		/*
		 * + + + + +
		 */
		draw(-1, -1);
		draw(-1, 1);
		draw(0, 0);
		draw(1, -1);
		draw(1, 1);
	}

	private void drawOffset1() {
		/*
		 * + +
		 * 
		 * + +
		 */
		draw(0, 0);
		draw(0, 2);
		draw(2, 1);
		draw(2, 3);
	}

	private void drawLine1() {
		/*
		 * +++
		 */
		draw(0, -1);
		draw(0, 0);
		draw(0, 1);
	}

	private void drawLine2() {
		/*
		 * + + +
		 */
		draw(0, -2);
		draw(0, 0);
		draw(0, 2);
	}

	private void drawLineCircle1() {
		/*
		 * + + + + + +
		 */
		draw(0, 0);
		draw(1, -1);
		draw(1, 1);
		draw(2, -1);
		draw(2, 1);
		draw(3, 0);
	}

	private void drawLineCircle2() {
		/*
		 * ++ + + + + ++
		 */
		draw(0, 0);
		draw(0, 1);
		draw(1, -1);
		draw(1, 2);
		draw(2, -1);
		draw(2, 2);
		draw(3, 0);
		draw(3, 1);
	}

	private void drawRectangle() {
		/*
		 * + + + + + +
		 */
		draw(-1, -1);
		draw(-1, 1);
		draw(0, -1);
		draw(0, 1);
		draw(1, -1);
		draw(1, 1);
	}

	private void draw3DFour1() {
		draw(0, 0);
		draw(0, 0);
		draw(0, 1);
		draw(0, 1);
		draw(1, 0);
		draw(1, 0);
		draw(1, 1);
		draw(1, 1);
	}


	private void drawLittleSphere1() {
		int[] list1 = { -1, 0, 1 };
		for (int i : list1) {
			for (int j : list1) {
				if (i != 0 || j != 0) {
					draw(i, j);
				}
			}
		}
	}

	private void drawLittleSphere2() {
		int[] list1 = { -2, 0, 2 };
		for (int i : list1) {
			for (int j : list1) {
				if (i != 0 || j != 0) {
					draw(i, j);
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
	private void drawTotallyRandom() {
		final int extent = 2+random.nextInt(5);
		final int count = 4+random.nextInt(20);
		for(int i=0;i<count;i++) {
			int x=random.nextInt(2*extent+1)-extent;
			int y=random.nextInt(2*extent+1)-extent;
			if (getShape(x,y)==null) {
				draw(x,y);
			}
		}
	}
	private void draw() {
		if (random.nextBoolean()) {
			drawTotallyRandom();
		}
		switch (random.nextInt(16)) {
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
			drawRectangle();
			break;
		case 14:
			drawLittleSphere1();
			break;
		case 15:
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
				//cameraXform.rz.setAngle(cameraXform.rz.getAngle() + 0.2);
				// running=false;
				if (nowInNanoSeconds - last[0] < delayBetweenStepsInMls * ONE_MILLISECOND_IN_NANOSECONDS) {
					return;
				}
				last[0] = nowInNanoSeconds;
				try {
					update();
				} catch (Exception exc) {
					exc.printStackTrace();
				}
				makeTitle();
				world.requestLayout();
				if (shapes.isEmpty()) {
					System.out.println("No more shapes");
					isRunning=false;
				}
				//isRunning=false; // temp
			}
		};
		timer.start();
	}

	// ------------
	@Override
	public void start(Stage stage) throws Exception {
		try {
			startAux(stage);
		} catch (Throwable thr) {
			thr.printStackTrace();
			System.exit(1);
		}
	}
	private void startAux(Stage stage) throws Exception {
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
