import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                // Compose draws under the status bar and home indicator and pads for them itself,
                // so each page's colour fills the whole screen; it handles the keyboard too.
                .ignoresSafeArea(.all)
    }
}
