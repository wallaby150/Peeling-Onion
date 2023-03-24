import 'package:flutter/material.dart';
import 'package:front/models/custom_models.dart';
import 'package:front/services/onion_api_service.dart';
import 'package:front/screens/postbox_screens/postbox_widgets/show_growing_onions.dart';
import '../../widgets/trash_can.dart';

class PackageScreen extends StatefulWidget {
  // 우선 user id 하는 식. 이후 토큰으로 api 변경될 예정
  final int id = 1;
  const PackageScreen({super.key});

  @override
  State<PackageScreen> createState() => _PackageScreenState();
}

class _PackageScreenState extends State<PackageScreen> {
  // 받아온 기르는 양파 정보들 (우선 홈 화면 api 연결.)
  // 나중에 택배함 api 완성되면 연결
  late Future<List<CustomHomeOnion>> onions;

  @override
  void initState() {
    super.initState();
    onions = OnionApiService.getGrowingOnionByUserId(widget.id);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: FutureBuilder(
          future: onions,
          builder: (context, AsyncSnapshot<dynamic> snapshot) {
            if (snapshot.hasData) {
              List<CustomHomeOnion> onionsData =
                  snapshot.data as List<CustomHomeOnion>;
              // 양파들 출력
              return Stack(
                children: [
                  ShowGrowingOnions(onions: onionsData),
                  const TrashCan(kind: 'onion'),
                ],
              );
            } else if (snapshot.hasError) {
              return Text('에러: ${snapshot.error}');
            }
            // 로딩 화면
            return const CircularProgressIndicator();
          },
        ),
      ),
    );
  }
}

/*// 
class ShowGrowingOnions extends StatefulWidget {
  final List<CustomHomeOnion> _onions;

  const ShowGrowingOnions({
    super.key,
    required List<CustomHomeOnion> onions,
  }) : _onions = onions;

  @override
  State<ShowGrowingOnions> createState() => _ShowGrowingOnionsState();
}

class _ShowGrowingOnionsState extends State<ShowGrowingOnions> {
  int onionsPerPage = 9;

  late int numOfPages = (widget._onions.length / onionsPerPage).ceil();

  @override
  Widget build(BuildContext context) {
    // 양파 수가 많으면, 페이지 넘겨서 확인
    return PageView.builder(
      itemCount: numOfPages,
      itemBuilder: (context, pageIndex) {
        // 선반 세로로 출력
        return Padding(
          padding: const EdgeInsets.all(8.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            // 선반 3개출력
            children: List.generate(
              3,
              (shelfIndex) {
                int onionsPerShelf = 3;
                int firstOnionIndex =
                    pageIndex * onionsPerPage + shelfIndex * onionsPerShelf;
                // 각 선반 1개
                return Column(
                  children: [
                    GridView.builder(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      itemCount: onionsPerShelf,
                      gridDelegate:
                          const SliverGridDelegateWithFixedCrossAxisCount(
                        crossAxisCount: 3,
                        mainAxisSpacing: 10,
                        crossAxisSpacing: 10,
                      ),
                      itemBuilder: (BuildContext context, int itemIndex) {
                        int globalIndex = firstOnionIndex + itemIndex;
                        if (globalIndex < widget._onions.length) {
                          // 각 양파 1개 (텍스트 + 이미지)
                          return DraggableOnion(
                              onions: widget._onions, globalIndex: globalIndex);
                        } else {
                          return const SizedBox.shrink();
                        }
                      },
                    ),
                    // 선반 1개 이미지
                    Image.asset(
                      'assets/images/shelf.png',
                    ),
                  ],
                );
              },
            ),
          ),
        );
      },
    );
  }
}

// 양파 drag & drop 기능
class DraggableOnion extends StatelessWidget {
  const DraggableOnion({
    super.key,
    required List<CustomHomeOnion> onions,
    required this.globalIndex,
  }) : _onions = onions;

  final List<CustomHomeOnion> _onions;
  final int globalIndex;

  @override
  Widget build(BuildContext context) {
    return Draggable<int>(
      // 전달할 데이터 (양파 번호)
      data: _onions.elementAt(globalIndex).onionId,
      // 드래그 할 때 양파 이미지만 투명해지게 이동하기. 이후 예쁘게 수정 예정
      feedback: SizedBox(
        width: 100,
        height: 100,
        child: Center(
          child: Transform.scale(
            scale: 1.2,
            child: Opacity(
              opacity: 0.6,
              child: Image.asset('assets/images/onion_image.png'),
            ),
          ),
        ),
      ),
      // 드래그할 때 보일 양파 이미지
      childWhenDragging: OneOnion(onions: _onions, globalIndex: globalIndex),
      // 겉으로 보일 양파
      child: OneOnion(onions: _onions, globalIndex: globalIndex),
    );
  }
}

// 양파 1개
class OneOnion extends StatelessWidget {
  const OneOnion({
    super.key,
    required List<CustomHomeOnion> onions,
    required this.globalIndex,
  }) : _onions = onions;

  final List<CustomHomeOnion> _onions;
  final int globalIndex;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(_onions.elementAt(globalIndex).onionName),
        Image.asset('assets/images/onion_image.png'),
      ],
    );
  }
}

*/