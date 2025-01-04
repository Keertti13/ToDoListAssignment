package com.mycompany.todolist_assignment;
import java.util.Scanner;
import java.io.*;
import java.util.*;
import java.text.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.InputMismatchException;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;

public class ToDoList_Assignment {
    
    private static final String FILE_NAME = "task.dat";
    
    public static void main(String[] args) {
        Scanner input = new Scanner (System.in);
        
        ArrayList<Task> listOfTasks = new ArrayList<>(); //Creates a new ArrayList to store the tasks
        StorageSystem.loadTasks(listOfTasks, FILE_NAME); // Load saved tasks
        
        System.out.println("Welcome to your To-Do List!");
        System.out.println("Before starting, please enter your email address for task notifications:");
        String userEmail = input.nextLine();
        System.out.println();

        VectorSearch vectorSearch = new VectorSearch();
        
        while (true) {
            int choice = getChoice(input);
            
            switch (choice) {
                case 1 -> taskAdder(input, listOfTasks);           
                case 2 -> displayTasks(listOfTasks);
                case 3 -> findTask(input, listOfTasks);
                case 4 -> fullTextSearch(input, listOfTasks);
                case 5 -> deleteTask(input, listOfTasks);
                case 6 -> markTaskComplete(input, listOfTasks);
                case 7 -> checkAndSendNotifications(userEmail, listOfTasks);
                case 8 -> {
                    try {
                        vectorSearch.indexTasks(listOfTasks);
                        searchWithVector(input, vectorSearch, listOfTasks);
                    } catch (Exception e) {
                        System.out.println("Vector search error: " + e.getMessage());
                    }
                }
                case 0 -> {
                    StorageSystem.saveTasksToFile(listOfTasks);
                    System.out.println("Goodbye!");
                    input.close();
                    return;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    //USER CHOICE
    private static int getChoice(Scanner input) {
        System.out.println("""
            ==========================
            What would you like to do?
            (1) Add a task
            (2) Output all tasks
            (3) Find a task by ID
            (4) Search tasks by keyword
            (5) Delete task
            (6) Mark Task as Complete
            (7) Send Notifications for Tasks Due in 24 Hours
            (0) Exit
            ==========================""");
            return input.nextInt();
    }

    private static void searchWithVector(Scanner input, VectorSearch vectorSearch, ArrayList<Task> listOfTasks) throws Exception {
        input.nextLine(); // clear newline
        System.out.print("Enter search query: ");
        String queryStr = input.nextLine();

        ArrayList<Task> results = vectorSearch.searchTasks(queryStr, listOfTasks);
        System.out.println("\n=== Vector Search Results ===");
        if (results.isEmpty()) {
            System.out.println("No tasks found matching the query.");
        } else {
            results.forEach(System.out::println);
        }
    }
    
    //TASK ADDER
    private static Task taskAdder(Scanner input, ArrayList<Task> listOfTasks) { //uses the ArrayList as a parameter, enabling taskAdder to access the list
        input.nextLine(); //clear the newline
        String title,description,dueDate,category,priorityLvl;
        
        //input title
        System.out.print("Enter task title: ");
        title = input.nextLine();
        //input description
        System.out.print("Enter task description: ");
        description = input.nextLine();
        //input dueDate
        while (true) {
            System.out.print("Enter due date (DD-MM-YYYY): ");
            dueDate = input.nextLine();
            if (dateChecker(dueDate)) {
                break;
            } else {
                System.out.println("Invalid date. Please enter a valid date in the format DD-MM-YYYY");
            }
        }
        //input category
        while (true) {
            System.out.print("Enter task category (Homework, Personal, Work): ");
            category= input.nextLine();
            category = category.substring(0, 1).toUpperCase() + category.substring(1);
            if (categoryChecker(category)) {
                break;
            } else {
                System.out.println("Invalid category. Please choose one of the three");
            }
        }
        //input priority
        while (true) {
            System.out.print("Priority level (Low, Medium, High): ");
            priorityLvl = input.nextLine();
            priorityLvl = priorityLvl.substring(0, 1).toUpperCase() + priorityLvl.substring(1);
            if (priorityChecker(priorityLvl)) {
                break;
            } else {
                System.out.println("Invalid priority level. Please choose one of the three");
            }
        }
        
        Task task = new Task(title,description,dueDate,category,priorityLvl); //creates a new Task object
        listOfTasks.add(task); //adds the new object into the ArrayList
        System.out.println("Task \"" + title + "\" added successfully!");
        
        return task;
    }
    
    //DATE CHECKER FOR TASKADDER
    private static boolean dateChecker(String dueDate) {
        String datePattern = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[0-2])-(\\d{4})$";
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        dateFormat.setLenient(false); //disables lenient parsing
        
        if (dueDate.matches(datePattern)) {
            try {
                dateFormat.parse(dueDate);
            } catch (ParseException e) {
                return false;
            }
        }
        
        return dueDate.matches(datePattern);
    }
    
    //CATEGORY CHECKER FOR TASKADDER
    private static boolean categoryChecker(String category) {
        return category.matches("Homework|Personal|Work");
    }
    
    //PRIORITY CHECKER FOR TASKADDER
    private static boolean priorityChecker(String priority) {
        return priority.matches("Low|Medium|High");
    }
    
    //TASK DISPLAYER
    private static void displayTasks(ArrayList<Task> listOfTasks) {
        if (listOfTasks.isEmpty()) {
            System.out.println("There's nothing here!");
        }
        for (Task task : listOfTasks) { //iterates through the ArrayList for each element in it
             System.out.println(task);
        }
    }

    // Additional functions for task operations and email notifications can remain as in the original.
    static class StorageSystem {
         public static void saveTasks(ArrayList<Task> listOfTasks, String fileName) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
                oos.writeObject(listOfTasks);
                System.out.println("Tasks saved successfully.");
            } catch (IOException e) {
                System.out.println("Error saving tasks: " + e.getMessage());
            }
        }

        @SuppressWarnings("unchecked")
        public static void loadTasks(ArrayList<Task> listOfTasks, String fileName) {
            File file = new File(fileName);
            if (!file.exists()) {
                System.out.println("No saved tasks found.");
                return;
            }

            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                listOfTasks.addAll((ArrayList<Task>) ois.readObject());
                System.out.println("Tasks loaded successfully.");
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Error loading tasks: " + e.getMessage());
            }
        }
    }

    static class VectorSearch {
        private final RAMDirectory index;
        private final StandardAnalyzer analyzer;

        public VectorSearch() {
            this.index = new RAMDirectory();
            this.analyzer = new StandardAnalyzer();
        }

        public void indexTasks(ArrayList<Task> listOfTasks) throws Exception {
            try (IndexWriter writer = new IndexWriter(index, new IndexWriterConfig(analyzer))) {
                for (Task task : listOfTasks) {
                    Document doc = new Document();
                    doc.add(new TextField("title", task.getTitle(), Field.Store.YES));
                    doc.add(new TextField("description", task.getDescription(), Field.Store.YES));
                    doc.add(new IntPoint("id", task.getId()));
                    doc.add(new StoredField("id", task.getId()));
                    writer.addDocument(doc);
                }
                System.out.println("Tasks indexed successfully.");
            }
        }
        public ArrayList<Task> searchTasks(String queryStr) throws Exception {
            ArrayList<Task> results = new ArrayList<>();
            Query query = new QueryParser("title", analyzer).parse(queryStr);

            try (IndexReader reader = DirectoryReader.open(index)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                TopDocs docs = searcher.search(query, 10);

                for (ScoreDoc scoreDoc : docs.scoreDocs) {
                    Document doc = searcher.doc(scoreDoc.doc);
                    results.add(new Task(doc.get("title"), doc.get("description"), doc.get("dueDate"), "", ""));
                }
            }

            return results;
        }
    }
    
    //TASK FINDER
    private static void findTask(Scanner scanner, ArrayList<Task> listOfTasks) {
        int id;
        System.out.println("What is the ID of the task?");
        while (true) 
        {
            try {
                
                id = scanner.nextInt(); 
                scanner.nextLine(); 
                break; 
            } catch (InputMismatchException e) {
                System.out.println("Invalid input! Please enter a valid integer ID.");
                scanner.nextLine(); 
            }
        }
        Task task = findTaskById(listOfTasks, id);
        if (task == null) {
            System.out.println("There is no task with this ID!");
        } else {
            System.out.println(task);
        }
    }
    
    //TASK FINDER BY ID
    private static Task findTaskById(ArrayList<Task> listOfTasks, int id) {
        for (Task task : listOfTasks) {
            if (task.getId() == id) {
                return task;
            }
        }
        return null;
    }

    //TASK FINDER BY TITLE OR DESCRIPTION
    private static void fullTextSearch(Scanner input, ArrayList<Task> listOfTasks) {
        System.out.print("Enter a keyword to search by title or description: ");
        input.nextLine();
        String keyword = input.nextLine().toLowerCase();
        boolean found = false;
        Iterator<Task> iterator = listOfTasks.iterator();
    
        System.out.println("Searching for tasks matching the keyword \"" + keyword + "\":");
        System.out.println("\n=== Search Results ===");
        while (iterator.hasNext()) 
        {
            Task task = iterator.next();
            if (task.getTitle().toLowerCase().contains(keyword) || task.getDescription().toLowerCase().contains(keyword)) 
            {
                System.out.println(task);
                found = true;
            }   
        }
        if (!found) {
            System.out.println("No tasks found matching the keyword \"" + keyword + "\".");
            System.out.println();
        }
    }
    
    //TASK DELETER
    private static void deleteTask(Scanner input, ArrayList<Task> listOfTasks) {
        String title;
        Iterator<Task> iterator = listOfTasks.iterator();
        System.out.println("Which task would you like to delete? (Enter ID)");
        int id = input.nextInt();
        boolean found = false;
        
        while (iterator.hasNext()) {
            Task task = iterator.next();
            
            if (task.getId() == id) {
                title = task.getTitle();
                iterator.remove();
                System.out.println("Task \"" + title + "\" deleted successfully!");
                found = true;
                break;
            }
        }
        if (!found) {
            System.out.println("Task with ID " + id + " not found.");
        }
    }
    
    //MARK TASK AS COMPLETE
    public static void markTaskComplete(Scanner input , ArrayList<Task> listOfTasks) {
        Iterator<Task> iterator = listOfTasks.iterator();
        System.out.println("Which task would you like to mark as complete? (Enter ID)");
        int id = input.nextInt();
        boolean found = false;
        while (iterator.hasNext()) 
        {
            Task task = iterator.next();
            if(task.getId() == id)
            {
                task.markComplete();
                System.out.println("Task \"" + task.getTitle() + "\" marked as completed");                    
                found = true;
                break;
            }
            
        }
        if(!found) {
            System.out.println("Task with ID " + id + " not found.");
        }
    }

    //CHECK FOR TASK THAT DUE WITHIN 24 HOURS
    private static boolean isTaskDueWithin24Hours(String dueDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        dateFormat.setLenient(false);
        try {
            Date taskDate = dateFormat.parse(dueDate);
            Date currentDate = new Date();
            long difference = taskDate.getTime() - currentDate.getTime();
            return difference > 0 && difference <= 24 * 60 * 60 * 1000; // Within 24 hours
        } catch (ParseException e) {
            return false;
        }
    }

    //SEND EMAIL NOTIFICATION
    private static void sendEmail(String userEmail, Task task) {
        String host = "smtp.gmail.com"; 
        String from = "yiwenlai0502@gmail.com"; 
        String password = "yhvx clmn qhdz ansa"; 

        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(userEmail));
            message.setSubject("Task Reminder: " + task.getTitle());
            message.setText("Hello,\n\nThis is a friendly reminder that the task \"" + task.getTitle() 
                            + "\" is due within the next 24 hours.\n\nTask Details:\n"
                            + "Description: " + task.getDescription() + "\n"
                            + "Due Date: " + task.getDueDate() + "\n\n"
                            + "Please ensure to complete it on time.\n\nThank you!");

            Transport.send(message);
            System.out.println("Reminder email sent successfully to: " + userEmail + " for task \"" + task.getTitle() + "\" due in 24 hours.");
        } catch (MessagingException e) {
            System.out.println("Failed to send email: " + e.getMessage());
        }
    }

    //CHECK AND SEND NOTIFICATIONS
    private static void checkAndSendNotifications(String userEmail, ArrayList<Task> listOfTasks) {
        boolean emailSent = false;
        for (Task task : listOfTasks) 
        {
            if (isTaskDueWithin24Hours(task.getDueDate())) 
            {
                sendEmail(userEmail, task);
                emailSent = true;
            }
        }
        if (!emailSent) 
        {
            System.out.println("No tasks are due within the next 24 hours.");
        }
    }
}
