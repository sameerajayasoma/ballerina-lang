
public function testInitAbstractObject ()  {
    Person1 p1 = new Person1();
    Person2 p2 = new Person2();
}

public function testInitAbstractObjectWithNew () {
    Person1 p1 = new;
    Person2 p2 = new;
}

type Person1 abstract object {
    public int age;
    public string name;

    int year;
    string month;
};

// Abstract object with constructor method
type Person2 abstract object {
    public int age;
    public string name;

    int year = 50;
    string month = "february";
    
    new () {
    }
};

// Non-abstract object with a function that has no implementation
type Person3 object {
    public int age;
    public string name;

    int year;
    string month;

    public function getName() returns string;
};

// Abstract object with method definition
type Person4 abstract object {
    public int age;
    public string name;

    int year;
    string month;

    public function getName() returns string {
        return name;
    }
};

// Attach function to an abstract object
type Person5 abstract object {
    public int age;
    public string name;

    int year;
    string month;

    public function getName() returns string;
};

function Person5::getName() returns string {
    return "my name";
}
