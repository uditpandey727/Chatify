import java.util.Scanner;

public class Main {

  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);
    System.out.print("Enter a string: ");
    String input = scanner.nextLine();
    input = input.replace("https://d1d34p8vz63oiq.cloudfront.net/", "");
    int index = input.indexOf("/");

    if (index != -1) {
      input = input.substring(0, index);
    }
    System.out.println("Modified string: " + input);
  }
}