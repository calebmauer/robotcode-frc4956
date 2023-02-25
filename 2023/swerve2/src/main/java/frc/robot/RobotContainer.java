package frc.robot;

import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.networktables.GenericEntry;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.commands.DefaultDriveCommand;
import frc.robot.subsystems.Drivetrain;
import static frc.robot.Constants.OperatorConstants.*;

import java.util.Map;

/**
 * This class is where the bulk of the robot is declared, including subsystems, default commands,
 * controller button bindings, etc.
 */
public class RobotContainer {
  
  private final Drivetrain drivetrain = new Drivetrain();
  public boolean fieldrelative = true;

  private final CommandXboxController driverController = new CommandXboxController(kDriverControllerPort);
  private ShuffleboardTab drivetab = Shuffleboard.getTab("Drive");
  
  
  private GenericEntry maxspeed =
    drivetab
      .add("Max Speed", .25)
      .withWidget(BuiltInWidgets.kNumberSlider)
     .withProperties(Map.of("min", 0, "max", 1))
      .getEntry();
  private GenericEntry fieldrelative2 =
    drivetab
      .add("Field Relative", false)
      .withWidget(BuiltInWidgets.kToggleSwitch)
      .getEntry();
 // private resetgyro = 
    //drivetrainSubsystem .add ("Gyro Reset", )

  private ShuffleboardTab autotab = Shuffleboard.getTab("Autonomous");    

  private final SlewRateLimiter Xfilter = new SlewRateLimiter(10);
    private final SlewRateLimiter Yfilter = new SlewRateLimiter(10);
    private final SlewRateLimiter Rfilter = new SlewRateLimiter(10);

  /**
   * The container for the robot. Contains subsystems, OI devices, and commands.
   */
  public RobotContainer() {
    // Set up the default command for the drivetrain.
    // The controls are for field-oriented driving:
    // Left stick Y axis -> forward and backwards movement
    // Left stick X axis -> left and right movement
    // Right stick X axis -> rotation
    
    drivetrain.setDefaultCommand(new DefaultDriveCommand(
            drivetrain,
            () -> -modifyAxis(Xfilter.calculate(driverController.getLeftY())) * Drivetrain.MAX_VELOCITY_METERS_PER_SECOND,
            () -> -modifyAxis(Yfilter.calculate(driverController.getLeftX())) * Drivetrain.MAX_VELOCITY_METERS_PER_SECOND,
            () -> -modifyAxis(Rfilter.calculate(driverController.getRightX())) * Drivetrain.MAX_ANGULAR_VELOCITY_RADIANS_PER_SECOND,
            () -> fieldrelative2.getBoolean(false),
            () -> maxspeed.getDouble(.25)
    ));
    drivetab
      .addNumber ("Voltage", () -> RobotController.getBatteryVoltage())
      .withWidget(BuiltInWidgets.kVoltageView)
      .withProperties(Map.of("min", 0, "max", 13));
      putDriveControls();

    // Configure the button bindings
    configureButtonBindings();
  }

  /**
   * Use this method to define your button->command mappings. Buttons can be created by
   * instantiating a {@link GenericHID} or one of its subclasses ({@link
   * edu.wpi.first.wpilibj.Joystick} or {@link XboxController}), and then passing it to a {@link
   * edu.wpi.first.wpilibj2.command.button.JoystickButton}.
   */
  private void configureButtonBindings() {
    
    // Back button zeros the gyroscope
    // No requirements because we don't need to interrupt anything
    Trigger backButton = driverController.back();
    backButton.onTrue(new InstantCommand(drivetrain::zeroGyroscope));
  }
  /** 
   * Add a list of controls to the SmartDashboard
   */
  private void putDriveControls(){
    ShuffleboardTab tab = Shuffleboard.getTab("Controls");
    tab
      .add("Reset Gyro", "Back Button");
  }


  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand() {
    // A command that does nothing
    return Commands.run(()->{
      drivetrain.drive(
                        new ChassisSpeeds(
                           0,
                           1,
                           0    
                        )
                );

    },drivetrain).withTimeout(5);
  }

  private static double deadband(double value, double deadband) {
    if (Math.abs(value) > deadband) {
      if (value > 0.0) {
        return (value - deadband) / (1.0 - deadband);
      } else {
        return (value + deadband) / (1.0 - deadband);
      }
    } else {
      return 0.0;
    }
  }

  private static double modifyAxis(double value) {
    // Deadband
    value = deadband(value, 0.05);

    // Square the axis
    value = Math.copySign(value * value, value);

    return value;
  }
}