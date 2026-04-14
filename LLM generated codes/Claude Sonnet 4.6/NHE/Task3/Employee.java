public class Employee {

    String  name;
    int     id;
    String  department;
    double  salary;
    boolean active;

    public Employee(String name, int id, String department,
                    double salary, boolean active) {
        this.name       = name;
        this.id         = id;
        this.department = department;
        this.salary     = salary;
        this.active     = active;
    }

    public String  getName()       { return name;       }
    public int     getId()         { return id;         }
    public String  getDepartment() { return department; }
    public double  getSalary()     { return salary;     }
    public boolean isActive()      { return active;     }

    public String getSummary() {
        return "Employee[" + id + "] " + name
             + " | dept=" + department
             + " | salary=" + salary
             + " | active=" + active;
    }

    @Override
    public String toString() {
        return getSummary();
    }
}