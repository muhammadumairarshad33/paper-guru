import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show rootBundle;
import 'package:syncfusion_flutter_pdfviewer/pdfviewer.dart';

void main() {
  runApp(const PaperGuruApp());
}

class PaperGuruApp extends StatelessWidget {
  const PaperGuruApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Paper Guru',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const HomePage(),
    );
  }
}

// ==================== HOME PAGE ====================

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'Paper Guru',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        centerTitle: true,
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Text(
                'Select Class',
                style: TextStyle(
                  fontSize: 26,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 30),

              // 9th Class
              SizedBox(
                width: 300,
                height: 60,
                child: ElevatedButton(
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        // displayName is what the user sees.
                        // folderName MUST exactly match your folder under assets/papers/
                        builder: (context) => const SubjectPage(
                          displayName: '9th Class',
                          folderName: '9th',
                        ),
                      ),
                    );
                  },
                  child: const Text(
                    '9th Class',
                    style: TextStyle(fontSize: 20),
                  ),
                ),
              ),

              const SizedBox(height: 20),

              // 10th Class - now wired up (was empty before)
              SizedBox(
                width: 300,
                height: 60,
                child: ElevatedButton(
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => const SubjectPage(
                          displayName: '10th Class',
                          folderName: '10th',
                        ),
                      ),
                    );
                  },
                  child: const Text(
                    '10th Class',
                    style: TextStyle(fontSize: 20),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ==================== SUBJECT PAGE ====================

class SubjectPage extends StatelessWidget {
  final String displayName; // "9th Class" - shown to user
  final String folderName; // "9th" - used for asset paths

  const SubjectPage({
    super.key,
    required this.displayName,
    required this.folderName,
  });

  static const List<String> subjects = [
    'English',
    'Urdu',
    'Mathematics',
    'Physics',
    'Chemistry',
    'Biology',
    'Computer Science',
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(displayName),
        centerTitle: true,
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: subjects.length,
        itemBuilder: (context, index) {
          final subject = subjects[index];

          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            elevation: 3,
            child: ListTile(
              contentPadding: const EdgeInsets.symmetric(
                horizontal: 20,
                vertical: 8,
              ),
              leading: const CircleAvatar(
                child: Icon(Icons.book),
              ),
              title: Text(
                subject,
                style: const TextStyle(
                  fontSize: 19,
                  fontWeight: FontWeight.bold,
                ),
              ),
              subtitle: const Text(
                'Past Papers دیکھنے کے لیے click کریں',
              ),
              trailing: const Icon(
                Icons.arrow_forward_ios,
                size: 18,
              ),
              onTap: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => YearsPage(
                      displayName: displayName,
                      folderName: folderName,
                      subject: subject,
                    ),
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }
}

// ==================== YEARS PAGE ====================

class YearsPage extends StatelessWidget {
  final String displayName;
  final String folderName;
  final String subject;

  const YearsPage({
    super.key,
    required this.displayName,
    required this.folderName,
    required this.subject,
  });

  static const List<String> years = [
    '2024',
    '2023',
    '2022',
    '2021',
    '2020',
    '2019',
    '2018',
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(subject),
        centerTitle: true,
      ),
      body: Column(
        children: [
          const SizedBox(height: 20),
          Text(
            '$displayName - $subject',
            style: const TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.bold,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 8),
          const Text(
            'Past Paper کا سال منتخب کریں',
            style: TextStyle(
              fontSize: 16,
              color: Colors.grey,
            ),
          ),
          const SizedBox(height: 20),
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: years.length,
              itemBuilder: (context, index) {
                final year = years[index];

                return Card(
                  margin: const EdgeInsets.only(bottom: 12),
                  elevation: 3,
                  child: ListTile(
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 20,
                      vertical: 8,
                    ),
                    leading: const Icon(
                      Icons.calendar_month,
                      size: 32,
                      color: Colors.blue,
                    ),
                    title: Text(
                      '$year Past Paper',
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    trailing: const Icon(
                      Icons.arrow_forward_ios,
                      size: 18,
                    ),
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => PaperPage(
                            displayName: displayName,
                            folderName: folderName,
                            subject: subject,
                            year: year,
                          ),
                        ),
                      );
                    },
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

// ==================== PAST PAPER PAGE ====================

class PaperPage extends StatelessWidget {
  final String displayName;
  final String folderName;
  final String subject;
  final String year;

  const PaperPage({
    super.key,
    required this.displayName,
    required this.folderName,
    required this.subject,
    required this.year,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Past Paper'),
        centerTitle: true,
      ),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 700),
            child: Column(
              children: [
                const Icon(
                  Icons.description,
                  size: 80,
                  color: Colors.blue,
                ),
                const SizedBox(height: 20),
                Text(
                  displayName,
                  style: const TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  subject,
                  style: const TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  '$year Past Paper',
                  style: const TextStyle(
                    fontSize: 20,
                    color: Colors.grey,
                  ),
                ),
                const SizedBox(height: 30),
                Card(
                  elevation: 4,
                  child: Padding(
                    padding: const EdgeInsets.all(25),
                    child: Column(
                      children: [
                        const Icon(
                          Icons.picture_as_pdf,
                          size: 60,
                          color: Colors.red,
                        ),
                        const SizedBox(height: 15),
                        SizedBox(
                          width: double.infinity,
                          child: ElevatedButton.icon(
                            onPressed: () {
                              Navigator.push(
                                context,
                                MaterialPageRoute(
                                  builder: (context) => PdfViewerPage(
                                    folderName: folderName,
                                    subject: subject,
                                    year: year,
                                  ),
                                ),
                              );
                            },
                            icon: const Icon(Icons.visibility),
                            label: const Text('View Paper'),
                          ),
                        ),
                        const SizedBox(height: 12),
                        SizedBox(
                          width: double.infinity,
                          child: OutlinedButton.icon(
                            onPressed: () {
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(
                                  content: Text(
                                    'Download feature اگلے مرحلے میں add ہو گا۔',
                                  ),
                                ),
                              );
                            },
                            icon: const Icon(Icons.download),
                            label: const Text('Download Paper'),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

// ==================== PDF VIEWER PAGE ====================

class PdfViewerPage extends StatelessWidget {
  final String folderName;
  final String subject;
  final String year;

  const PdfViewerPage({
    super.key,
    required this.folderName,
    required this.subject,
    required this.year,
  });

  @override
  Widget build(BuildContext context) {
    // Expected file location:
    // assets/papers/<folderName>/<subject>/<subject>_<year>.pdf
    // Example: assets/papers/9th/Physics/Physics_2024.pdf
    final String assetPath =
        'assets/papers/$folderName/$subject/${subject}_$year.pdf';

    return Scaffold(
      appBar: AppBar(
        title: Text('$subject $year'),
      ),
      body: FutureBuilder<bool>(
        future: _assetExists(assetPath),
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.data == false) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  'یہ paper ابھی upload نہیں ہوا۔\n\nExpected file:\n$assetPath',
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 16),
                ),
              ),
            );
          }
          return SfPdfViewer.asset(assetPath);
        },
      ),
    );
  }

  Future<bool> _assetExists(String path) async {
    try {
      await rootBundle.load(path);
      return true;
    } catch (_) {
      return false;
    }
  }
}
