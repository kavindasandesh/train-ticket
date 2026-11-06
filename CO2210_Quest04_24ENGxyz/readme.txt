CO2210 Programming Quest - Quest 04
Sri Lanka Railways - Express Train Booking Management System (Java Swing GUI)

GROUP MEMBERS (fill in before submitting)
  1. [Initials Surname]   [Index number, e.g. 24ENG123]
  2. [Initials Surname]   [Index number]
  3. [Initials Surname]   [Index number]

WHAT THE PROGRAM DOES
  One window with four tabs, all built in code (no GUI-builder canvas):
    Timetable         Express trains by route and direction (outbound and return),
                      halts with times, and the carriages of each train.
    New Booking       Choose train, date, boarding/alighting station and class, enter
                      ID number, name and contact number, then click free seats on the
                      seat map (or use Auto-select) and press "Confirm booking".
    Manage Bookings   Search by booking ID or customer ID. Alter (opens the booking in
                      the New Booking tab) or Cancel the selected booking.
    Train Bookings    Seat arrangement of a whole train for a date plus the full list
                      of its bookings with customer details.

  Seat arrangement: every train has its own number of carriages and its own number of
  seats per carriage (see data/trains.csv). Carriages are drawn side by side like the
  train. Colours:  green = free, blue = selected, red = booked, grey = other class.
  Hover over a red seat to see who booked it.

  Rules: a customer can hold at most 4 seats on a train and date; a fully booked class
  cannot be booked; a seat can be sold again for a part of the journey that does not
  overlap an existing booking; bookings open 30 days in advance; payments are not handled.

FILES
  src/railway/Main.java            starts the application
  src/railway/model/               Train, Carriage, Seat, Booking, Passenger, Route, ...
  src/railway/data/                CSV reading/writing (TimetableRepository, BookingRepository)
  src/railway/service/             BookingService (all business rules), BookingException
  src/railway/util/Validator.java  input validation
  src/railway/ui/                  Swing screens (MainFrame, BookingPanel, SeatMapPanel, ...)
  data/trains.csv                  timetable (18 trains, carriages, halts)
  data/bookings.csv                booking records (created/updated by the program)

HOW TO RUN  (Java 11 or later)
  Command line, from this folder:
      mkdir out
      javac -d out $(find src -name "*.java")        (Windows: dir /s /b src\*.java > files.txt, then javac -d out @files.txt)
      java -cp out railway.Main
  NetBeans: create a Java Application project, copy the "railway" folder into its source
  folder (src), copy the "data" folder into the project root, set railway.Main as the
  main class and run. The program must be started from the folder that contains "data".

TIP: File > Add demo bookings creates sample bookings dated in the next few days so the
seat map has something to show.
