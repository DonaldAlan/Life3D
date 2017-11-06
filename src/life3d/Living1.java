package life3d;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
/**
 * 
 * @author Donald A. Smith, ThinkerFeeler@gmail.com
 *
 */
public class Living1 extends Application {
	private static boolean useRandomMaterials=false; // false makes it use hsb colors
	private static NumberFormat numberFormat = NumberFormat.getInstance();
	private static int maxIJK=12
			;
	private static int ruleIndex = 5;
	
	private final static int NUMBER_OF_RANDOM_COLORS=200;
	private final static int NUMBER_OF_HSB_COLORS=20;
	private final static int width = 1600;
	private final static int height = 900;
	private static final long ONE_MILLISECOND_IN_NANOSECONDS = 1000L*1000L;
	private static final long ONE_SECOND_IN_NANOSECONDS = 1000L*1000L*1000L;
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
	private static final Point3D YAXIS = new Point3D(0, 1, 0);
	private static List<PhongMaterial> randomMaterials = new ArrayList<>(NUMBER_OF_RANDOM_COLORS);
	private static List<PhongMaterial> hsbMaterials = new ArrayList<>(NUMBER_OF_HSB_COLORS);
	private volatile Sphere selectedSphere=null;
	private volatile boolean running=true;
	private PointLight light1;
	private PointLight light2;
	private Set<Shape3D> shapes = new HashSet<Shape3D>();
	private Map<Integer,Map<Integer,Map<Integer,Shape3D>>> shapeMap = new HashMap<Integer,Map<Integer,Map<Integer,Shape3D>>>();
	Stage primaryStage;
	
	//--------------
	static {
		randomizeColorsAndMaterials();
	}
	private double maxDistance = 100;
	private double halfMaxDistance = maxDistance/2;
	private double accelerationFactor = 0.1;
	private double velocityFactor=1.0;
	private double halfVelocityFactor= 0.5*velocityFactor;
	private class MyBox extends Box {
		private double velocityX=velocityFactor*random.nextDouble() - halfVelocityFactor;
		private double velocityY=velocityFactor*random.nextDouble() - halfVelocityFactor;
		private double velocityZ=velocityFactor*random.nextDouble() - halfVelocityFactor;
		public MyBox(double width, double height,double depth,  double x, double y, double z, PhongMaterial material) {
			super(width,height,depth);
			setTranslateX(x);
			setTranslateY(y);
			setTranslateZ(z);
			setMaterial(material);
			setRotationAxis(new Point3D(random.nextDouble(),random.nextDouble(),random.nextDouble()));
			//System.out.println(x + " " + y + " " + z + ",  " + velocityX + ", " + velocityY + ", " + velocityZ);
		}
		public void update(long timeInNanoSeconds) {
			setTranslateX(getTranslateX() + velocityX);
			setTranslateY(getTranslateY() + velocityY);
			setTranslateZ(getTranslateZ() + velocityZ);
			velocityX += accelerationFactor*minusOneOrOneDependingOnDistance(getTranslateX());
			velocityY += accelerationFactor*minusOneOrOneDependingOnDistance(getTranslateY());
			velocityZ += accelerationFactor*minusOneOrOneDependingOnDistance(getTranslateZ());
			setRotate(getRotate()+5);
		}
		// return positive or negative. The larger the absolute value of distance, the more likely it should move in
		private int minusOneOrOneDependingOnDistance(double offset) {
			double d = Math.abs(offset)/maxDistance;
			int signOfOffset = offset<0? -1 : 1;
			return random.nextDouble() > d ? signOfOffset : -signOfOffset;
		}
	}
	//---------------------------------
	private static void randomizeColorsAndMaterials() {
		randomMaterials.clear();
		for (int i = 0; i < NUMBER_OF_RANDOM_COLORS; i++) {
			Color color =randomColorExpensive();
			PhongMaterial pm= new PhongMaterial();
			//pm.setSpecularColor(randomColorExpensive());
			pm.setDiffuseColor(color);
			randomMaterials.add(pm);
		}
		for(int i=0;i<NUMBER_OF_HSB_COLORS;i++) {
			Color color =  Color.hsb((360.0*i)/NUMBER_OF_HSB_COLORS, 1,1); ;
			PhongMaterial pm= new PhongMaterial();
			pm.setDiffuseColor(color);
			hsbMaterials.add(pm);
		}
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
		final Rotate rx = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
		final Rotate ry = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
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
		if (selectedSphere!=null) {
		     selectedSphere.setScaleX(1);
             selectedSphere.setScaleY(1);
             selectedSphere.setScaleZ(1);
             selectedSphere=null;
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
//				  selectedSphere = (Sphere) pr.getIntersectedNode();
//				  selectedSphere.setScaleX(bigSize);
//				  selectedSphere.setScaleY(bigSize);
//				  selectedSphere.setScaleZ(bigSize);
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
//				cameraXform.ry.setAngle(cameraXform.ry.getAngle() + mouseDeltaX * 0.2);
//				cameraXform.rx.setAngle(cameraXform.rx.getAngle() - mouseDeltaY * 0.2);

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
					running = !running;
					break;
				case Q:
					System.exit(0);
					break;
				case DIGIT0: ruleIndex=0; makeTitle(); break;
				case DIGIT1: ruleIndex=1; makeTitle(); break;
				case DIGIT2: ruleIndex=2; makeTitle(); break;
				case DIGIT3: ruleIndex=3; makeTitle(); break;
				case DIGIT4: ruleIndex=4; makeTitle(); break;
				case DIGIT5: ruleIndex=5; makeTitle(); break;
				case DIGIT6: ruleIndex=6; makeTitle(); break;
				case DIGIT7: ruleIndex=7; makeTitle(); break;
				case DIGIT8: ruleIndex=8; makeTitle(); break;
				case DIGIT9: ruleIndex=9; makeTitle(); break;
				case R:
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
						camera.setTranslateZ(camera.getTranslateZ()+10);
					}
					break;
				case DOWN:
					if (ke.isShiftDown()) {
						camera.setTranslateY(camera.getTranslateY() - 10);
					} else {
						camera.setTranslateZ(camera.getTranslateZ()-10);
					}
					break;
				case C:
						world.requestLayout();
						break;
				case I:
						world.getChildren().removeAll(shapes);
						shapes.clear();
						shapeMap.clear();
						draw();
					break;
				default:
				}
			}
		});
	}

	private void animate() {
		final long last[] = {0};
		final AnimationTimer timer= new AnimationTimer() {
			@Override
			public void handle(long nowInNanoSeconds) {
				if (!running) {
					return;
				}
//				running=false;
				if (nowInNanoSeconds-last[0]<20*ONE_MILLISECOND_IN_NANOSECONDS) {
					return;
				}
				last[0]= nowInNanoSeconds;
				update(nowInNanoSeconds);
				makeTitle();
				world.requestLayout();
				if (shapes.isEmpty()) {
				}
			}
		};
		timer.start();
	}
	private void update(long timeInNanoSeconds) {
		for(Shape3D shape:shapes) {
			MyBox myBox = (MyBox) shape;
			myBox.update(timeInNanoSeconds);
		}
	}
	private double oneOrMinusOne() {
		return random.nextBoolean() ? -1.0 : 1.0;
	}
	private void makeTitle() {
		primaryStage.setTitle("Life3D with " + numberFormat.format(shapes.size()) + " nodes");
	}
	//-------
	private double randomPos() {
		return maxDistance*random.nextDouble() - halfMaxDistance;
	}
	//-----------
	private void draw() {
		for(int i=0;i<1000;i++) {
			PhongMaterial material = randomMaterials.get(random.nextInt(randomMaterials.size()));
			MyBox myBox = new MyBox(4,4,4, randomPos(),randomPos(),randomPos(), material);
			world.getChildren().add(myBox);
			shapes.add(myBox);
		}
	}
	//------------
	@Override
	public void start(Stage stage) throws Exception {
		primaryStage=stage;
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
		makeTitle();
		draw();
		animate();
	}

	public static void main(String[] args) {
		try {
			launch(args);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}
}
